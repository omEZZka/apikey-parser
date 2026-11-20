package com.aai.assistant.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aai.assistant.ui.theme.AccentCyan
import com.aai.assistant.ui.theme.AccentGreen
import com.aai.assistant.ui.theme.AccentViolet
import com.aai.assistant.ui.theme.BgDark
import com.aai.assistant.ui.theme.OutlineDim
import com.aai.assistant.ui.theme.SurfaceHigh
import com.aai.assistant.ui.theme.TextSecondary
import kotlinx.coroutines.delay

sealed class MdBlock {
    data class Code(val language: String, val code: String) : MdBlock()
    data class TextBlock(val text: String) : MdBlock()
}

fun parseMarkdownBlocks(markdown: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val lines = markdown.lines()
    var i = 0
    val textBuffer = StringBuilder()
    fun flushText() {
        if (textBuffer.isNotEmpty()) {
            blocks.add(MdBlock.TextBlock(textBuffer.toString().trimEnd()))
            textBuffer.clear()
        }
    }
    while (i < lines.size) {
        val line = lines[i]
        if (line.trimStart().startsWith("```")) {
            flushText()
            val lang = line.trimStart().removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            blocks.add(MdBlock.Code(lang, codeLines.joinToString("\n")))
            i++
        } else {
            textBuffer.appendLine(line)
            i++
        }
    }
    flushText()
    return blocks
}

private val inlinePattern = Regex(
    """(\*\*(.+?)\*\*)|(`([^`]+?)`)|(\*(?!\*)(.+?)\*)|(~~(.+?)~~)|(\[([^\]]+)\]\((https?://[^)]+)\))"""
)

fun renderInline(text: String): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (match in inlinePattern.findAll(text)) {
        append(text.substring(last, match.range.first))
        when {
            match.groups[2] != null -> withStyle(
                SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFFF4F7FF))
            ) { append(match.groups[2]!!.value) }
            match.groups[4] != null -> withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    color = AccentCyan,
                    background = Color(0x224FD8EB),
                    fontSize = 13.sp
                )
            ) { append(match.groups[4]!!.value) }
            match.groups[6] != null -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                append(match.groups[6]!!.value)
            }
            match.groups[8] != null -> withStyle(
                SpanStyle(textDecoration = TextDecoration.LineThrough, color = TextSecondary)
            ) { append(match.groups[8]!!.value) }
            match.groups[10] != null -> withStyle(
                SpanStyle(color = AccentViolet, textDecoration = TextDecoration.Underline)
            ) { append(match.groups[10]!!.value) }
        }
        last = match.range.last + 1
    }
    append(text.substring(last))
}

@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    val blocks = parseMarkdownBlocks(markdown)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Code -> CodeBlock(block.language, block.code)
                is MdBlock.TextBlock -> TextLines(block.text)
            }
        }
    }
}

@Composable
private fun TextLines(text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        text.lines().forEach { rawLine ->
            val line = rawLine.trimEnd()
            when {
                line.startsWith("### ") -> Text(
                    renderInline(line.removePrefix("### ")),
                    style = MaterialTheme.typography.titleMedium,
                    color = AccentCyan
                )
                line.startsWith("## ") -> Text(
                    renderInline(line.removePrefix("## ")),
                    style = MaterialTheme.typography.titleLarge,
                    color = AccentCyan
                )
                line.startsWith("# ") -> Text(
                    renderInline(line.removePrefix("# ")),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                line.trim() == "---" || line.trim() == "***" -> Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(OutlineDim)
                )
                line.trimStart().startsWith("> ") -> Row {
                    Box(
                        Modifier
                            .width(3.dp)
                            .height(18.dp)
                            .background(AccentViolet)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        renderInline(line.trimStart().drop(2)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        fontStyle = FontStyle.Italic
                    )
                }
                line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> Row {
                    Text("•  ", color = AccentViolet, fontSize = 15.sp)
                    Text(
                        renderInline(line.trimStart().drop(2)),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Regex("""^\d+[.)]\s""").containsMatchIn(line.trimStart()) -> Row {
                    val trimmed = line.trimStart()
                    val num = trimmed.takeWhile { it.isDigit() || it == '.' || it == ')' }
                    Text("$num ", color = AccentViolet, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(
                        renderInline(trimmed.drop(num.length).trimStart()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                line.isBlank() -> Spacer(Modifier.width(2.dp))
                else -> Text(
                    renderInline(line),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun CodeBlock(language: String, code: String) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1600)
            copied = false
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BgDark)
            .border(1.dp, OutlineDim, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceHigh.copy(alpha = 0.5f))
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(50))
                    .background(AccentViolet)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                language.ifBlank { "code" },
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = TextSecondary,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("code", code))
                    copied = true
                },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    aaiIcon(if (copied) AaiIcons.Check else AaiIcons.Copy),
                    contentDescription = "Копировать код",
                    tint = if (copied) AccentGreen else TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
        Text(
            code,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            color = Color(0xFF9CE8C3),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        )
    }
}