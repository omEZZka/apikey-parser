package com.aai.assistant.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aai.assistant.ui.theme.AccentCyan
import com.aai.assistant.ui.theme.AgentGradient
import com.aai.assistant.ui.theme.OutlineDim
import com.aai.assistant.ui.theme.TextSecondary

data class Suggestion(val emoji: String, val title: String, val prompt: String)

val defaultSuggestions = listOf(
    Suggestion("👁", "Что на экране?", "Опиши, что сейчас открыто на экране устройства."),
    Suggestion("🔎", "Найди в интернете", "Найди в интернете последние новости про Android и кратко перескажи 3 главных."),
    Suggestion("📶", "Выключи Bluetooth", "Открой настройки и выключи Bluetooth."),
    Suggestion("📝", "Список дел", "Создай файл /sdcard/download/todo.txt со списком дел на сегодня и покажи его."),
    Suggestion("🔋", "Состояние устройства", "Через shell узнай уровень заряда, свободную память и модель устройства. Кратко отчитайся."),
    Suggestion("🌤", "Погода", "Узнай через интернет текущую погоду в Москве и кратко расскажи."),
    Suggestion("📂", "Файлы загрузок", "Покажи список файлов в /sdcard/download и их размеры.")
)

@Composable
fun SuggestionChips(onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(defaultSuggestions) { s ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, OutlineDim, RoundedCornerShape(50))
                    .clickable { onPick(s.prompt) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(s.emoji, fontSize = 14.sp)
                Spacer(Modifier.width(7.dp))
                Text(
                    s.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun AgentAvatar(size: Int = 40, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape((size / 3).dp))
            .background(AgentGradient),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "a",
            fontSize = (size * 0.55f).sp,
            fontWeight = FontWeight.Black,
            color = androidx.compose.ui.graphics.Color(0xFF07090F)
        )
    }
}