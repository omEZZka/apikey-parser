package com.aai.assistant.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aai.assistant.data.db.MessageEntity
import com.aai.assistant.ui.chat.ChatUiState
import com.aai.assistant.ui.components.MarkdownText
import com.aai.assistant.ui.theme.AccentRed

@Composable
fun OverlayPanel(
    state: ChatUiState,
    onSend: (String) -> Unit,
    onClose: () -> Unit
) {
    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val visibleMessages = state.messages.filter {
        (it.role == "user" || it.role == "assistant" || it.role == "error") && it.content.isNotBlank()
    }.takeLast(12)

    LaunchedEffect(visibleMessages.size, visibleMessages.lastOrNull()?.content) {
        if (visibleMessages.isNotEmpty()) {
            listState.animateScrollToItem(visibleMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .imePadding()
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "aAi — быстрая задача",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            if (state.isRunning) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (visibleMessages.isNotEmpty()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(visibleMessages, key = { it.id }) { message ->
                    CompactMessage(message)
                }
            }
        } else {
            Text(
                "Например: «выключи Bluetooth» или «напиши в чат, что я задержусь». Агент выполнит задачу поверх текущего приложения.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Задача агенту…", fontSize = 13.sp) },
                maxLines = 3,
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (input.isNotBlank() && !state.isRunning) {
                        onSend(input)
                        input = ""
                    }
                },
                enabled = input.isNotBlank() && !state.isRunning,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (input.isNotBlank() && !state.isRunning) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Отправить",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun CompactMessage(message: MessageEntity) {
    when (message.role) {
        "user" -> Text(
            "Вы: ${message.content}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary
        )
        "error" -> Text(message.content, fontSize = 12.sp, color = AccentRed)
        else -> MarkdownText(message.content)
    }
}