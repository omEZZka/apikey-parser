package com.aai.assistant.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aai.assistant.ui.theme.AccentBlue

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

private val inlinePattern = Regex("""(\*\*(.+?)\*\*)|(`([^`]+?)`)|(\*(?!\*)(.+?)\*)""")

fun renderInline(text: String): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (match in inlinePattern.findAll(text)) {
        append(text.substring(last, match.range.first))
        when {
            match.groups[2] != null -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(match.groups[2]!!.value)
            }
            match.groups[4] != null -> withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = Color(0x3358A6FF),
                    fontSize = 13.sp
                )
            ) { append(match.groups[4]!!.value) }
            match.groups[6] != null -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                append(match.groups[6]!!.value)
            }
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
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                line.startsWith("## ") -> Text(
                    renderInline(line.removePrefix("## ")),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                line.startsWith("# ") -> Text(
                    renderInline(line.removePrefix("# ")),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> Row {
                    Text("•  ", color = AccentBlue, fontSize = 15.sp)
                    Text(
                        renderInline(line.trimStart().drop(2)),
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Regex("""^\d+[.)]\s""").containsMatchIn(line.trimStart()) -> Row {
                    val trimmed = line.trimStart()
                    val num = trimmed.takeWhile { it.isDigit() || it == '.' || it == ')' }
                    Text("$num ", color = AccentBlue, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(
                        renderInline(trimmed.drop(num.length).trimStart()),
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                line.isBlank() -> Spacer(Modifier.width(2.dp))
                else -> Text(
                    renderInline(line),
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun CodeBlock(language: String, code: String) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0A0E14))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1C2128))
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                language.ifBlank { "code" },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("code", code))
            }) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Копировать код",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(18.dp)
                )
            }
        }
        Text(
            code,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = Color(0xFF7EE787),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        )
    }
}