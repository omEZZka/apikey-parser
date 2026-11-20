package com.aai.assistant.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aai.assistant.ui.theme.AccentAmber
import com.aai.assistant.ui.theme.AccentGreen
import com.aai.assistant.ui.theme.AccentRed
import com.aai.assistant.ui.theme.BgDark
import com.aai.assistant.ui.theme.OutlineDim
import com.aai.assistant.ui.theme.TextMuted
import com.aai.assistant.ui.theme.TextSecondary

private fun toolLabel(name: String): String = when (name) {
    "get_screen_state" -> "Чтение экрана"
    "tap" -> "Нажатие"
    "long_press" -> "Долгое нажатие"
    "swipe" -> "Свайп"
    "type_text" -> "Ввод текста"
    "global_action" -> "Системное действие"
    "open_app" -> "Открытие приложения"
    "http_request" -> "HTTP-запрос"
    "web_search" -> "Поиск в интернете"
    "web_read" -> "Чтение страницы"
    "run_shell" -> "Shell-команда"
    "file_read" -> "Чтение файла"
    "file_write" -> "Запись файла"
    "file_list" -> "Список файлов"
    "file_delete" -> "Удаление файла"
    else -> name
}

@Composable
fun ToolCallCard(
    toolName: String,
    arguments: String,
    result: String?,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val running = result == null
    val statusColor = when {
        running -> AccentAmber
        isError -> AccentRed
        else -> AccentGreen
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, OutlineDim, RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(statusColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                if (running) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(15.dp),
                        strokeWidth = 2.dp,
                        color = statusColor
                    )
                } else {
                    Icon(
                        if (isError) Icons.Default.Close else Icons.Default.Check,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    toolLabel(toolName),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    toolName,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TextMuted,
                    maxLines = 1
                )
            }
            Text(
                when {
                    running -> "выполняется"
                    isError -> "ошибка"
                    else -> "готово"
                },
                style = MaterialTheme.typography.labelSmall,
                color = statusColor
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Свернуть" else "Развернуть",
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
        if (expanded) {
            Column(
                modifier = Modifier.padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DetailSection("Аргументы", prettyJson(arguments), Color(0xFF9CC5FF))
                if (result != null) {
                    DetailSection(
                        "Результат",
                        result.take(4000) + if (result.length > 4000) "\n…[обрезано]" else "",
                        if (isError) AccentRed else Color(0xFF8FE8B8)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailSection(title: String, body: String, color: Color) {
    Column {
        Text(title, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Spacer(Modifier.size(3.dp))
        Text(
            body,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            color = color,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(BgDark)
                .padding(10.dp)
        )
    }
}

fun prettyJson(raw: String): String = try {
    val format = kotlinx.serialization.json.Json { prettyPrint = true }
    format.encodeToString(
        kotlinx.serialization.json.JsonElement.serializer(),
        format.parseToJsonElement(raw)
    )
} catch (e: Exception) {
    raw
}