package com.aai.assistant.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aai.assistant.ui.theme.AccentGreen
import com.aai.assistant.ui.theme.AccentRed

@Composable
fun ToolCallCard(
    toolName: String,
    arguments: String,
    result: String?,
    isError: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val statusColor = when {
        result == null -> Color(0xFFD29922)
        isError -> AccentRed
        else -> AccentGreen
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF161B22))
            .clickable { expanded = !expanded }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                when {
                    result == null -> Icons.Default.HourglassTop
                    isError -> Icons.Default.Error
                    else -> Icons.Default.CheckCircle
                },
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.padding(end = 8.dp)
            )
            Icon(
                Icons.Default.Build,
                contentDescription = null,
                tint = Color(0xFF8B949E),
                modifier = Modifier.padding(end = 6.dp)
            )
            Text(
                toolName,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                color = Color(0xFFE6EDF3),
                modifier = Modifier.weight(1f)
            )
            Text(
                if (result == null) "выполняется…" else if (isError) "ошибка" else "готово",
                fontSize = 12.sp,
                color = statusColor
            )
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Свернуть" else "Развернуть",
                tint = Color(0xFF8B949E)
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text("Аргументы:", fontSize = 12.sp, color = Color(0xFF8B949E))
                Text(
                    prettyJson(arguments),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color(0xFF79C0FF),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0A0E14))
                        .padding(8.dp)
                )
                if (result != null) {
                    Text("Результат:", fontSize = 12.sp, color = Color(0xFF8B949E))
                    Text(
                        result.take(4000) + if (result.length > 4000) "\n…[обрезано]" else "",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = if (isError) AccentRed else Color(0xFF7EE787),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0A0E14))
                            .padding(8.dp)
                    )
                }
            }
        }
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