package com.aai.assistant.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aai.assistant.ui.components.aaiIcon
import com.aai.assistant.ui.components.AaiIcons
import com.aai.assistant.data.db.MessageEntity
import com.aai.assistant.data.media.ImageImporter
import com.aai.assistant.ui.chat.ChatUiState
import com.aai.assistant.ui.components.AgentAvatar
import com.aai.assistant.ui.components.MarkdownText
import com.aai.assistant.ui.theme.AccentAmber
import com.aai.assistant.ui.theme.AccentRed
import com.aai.assistant.ui.theme.AgentGradient
import com.aai.assistant.ui.theme.BgDark
import com.aai.assistant.ui.theme.BgElevated
import com.aai.assistant.ui.theme.DangerGradient
import com.aai.assistant.ui.theme.OutlineDim
import com.aai.assistant.ui.theme.SendGradient
import com.aai.assistant.ui.theme.TextMuted

@Composable
fun MiniChatPanel(
    state: ChatUiState,
    compact: Boolean,
    onToggleCompact: () -> Unit,
    onSend: (String, List<String>) -> Unit,
    onStop: () -> Unit,
    onClose: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onOpenApp: () -> Unit
) {
    var input by rememberSaveable { mutableStateOf("") }
    var attachedImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var pendingUris by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    var importing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(4)
    ) { uris -> if (uris.isNotEmpty()) pendingUris = uris }
    LaunchedEffect(pendingUris) {
        if (pendingUris.isNotEmpty()) {
            importing = true
            attachedImages = (attachedImages + ImageImporter.importAll(context, pendingUris)).take(4)
            pendingUris = emptyList()
            importing = false
        }
    }
    val listState = rememberLazyListState()
    val visibleMessages = state.messages.filter {
        (it.role == "user" || it.role == "assistant" || it.role == "error") && it.content.isNotBlank()
    }.takeLast(20)

    LaunchedEffect(visibleMessages.size, visibleMessages.lastOrNull()?.content) {
        if (visibleMessages.isNotEmpty()) {
            listState.animateScrollToItem(visibleMessages.size - 1)
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (compact) 0.92f else 1f,
        animationSpec = tween(250),
        label = "panelAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(BgElevated.copy(alpha = alpha))
            .border(1.dp, OutlineDim, RoundedCornerShape(24.dp))
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                }
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Box(modifier = Modifier.clickable(onClick = onOpenApp)) {
                AgentAvatar(size = 26)
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "aAi",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (state.isRunning) {
                    Text(
                        state.iterationInfo ?: "агент работает…",
                        fontSize = 9.sp,
                        color = AccentAmber,
                        maxLines = 1
                    )
                }
            }
            if (state.isRunning) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(DangerGradient)
                        .clickable(onClick = onStop)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(aaiIcon(AaiIcons.Stop),
                            contentDescription = "Стоп",
                            tint = Color(0xFF2B0B0B),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            "Стоп",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2B0B0B)
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
            }
            MiniHeaderButton(
                icon = if (compact) AaiIcons.UnfoldMore else AaiIcons.UnfoldLess,
                description = if (compact) "Развернуть историю" else "Свернуть в таблетку",
                onClick = onToggleCompact
            )
            Spacer(Modifier.width(4.dp))
            MiniHeaderButton(
                icon = AaiIcons.Close,
                description = "Закрыть мини-чат",
                onClick = onClose
            )
        }

        AnimatedVisibility(
            visible = !compact,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                if (visibleMessages.isNotEmpty()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp, max = 250.dp)
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(BgDark.copy(alpha = 0.6f))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(visibleMessages, key = { it.id }) { message ->
                            CompactMessage(message)
                        }
                        if (state.isRunning) {
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(11.dp),
                                        strokeWidth = 1.5.dp,
                                        color = AccentAmber
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        state.iterationInfo ?: "думаю…",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        "Напиши задачу — агент выполнит её поверх текущего приложения.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                    )
                }
            }
        }

        if (attachedImages.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                attachedImages.forEachIndexed { index, b64 ->
                    Box {
                        com.aai.assistant.ui.chat.MessageImage(b64, 52)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(0xCC07090F))
                                .clickable {
                                    attachedImages = attachedImages.filterIndexed { i, _ -> i != index }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(aaiIcon(AaiIcons.Close),
                                contentDescription = "Убрать",
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, OutlineDim, RoundedCornerShape(50))
                .padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 5.dp, top = 5.dp, start = 4.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(aaiIcon(AaiIcons.Attach),
                    contentDescription = "Прикрепить фото",
                    tint = if (attachedImages.isNotEmpty()) MaterialTheme.colorScheme.primary else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        if (compact) "Задача…" else "Задача агенту…",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                },
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                maxLines = if (compact) 2 else 4,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            val canSend = (input.isNotBlank() || attachedImages.isNotEmpty()) &&
                !state.isRunning && !importing
            Box(
                modifier = Modifier
                    .padding(bottom = 5.dp, top = 5.dp)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend) SendGradient
                        else androidx.compose.ui.graphics.SolidColor(BgDark)
                    )
                    .clickable(enabled = canSend) {
                        onSend(input, attachedImages)
                        input = ""
                        attachedImages = emptyList()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (state.isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = AccentAmber
                    )
                } else {
                    Icon(aaiIcon(AaiIcons.Send),
                        contentDescription = "Отправить",
                        tint = if (canSend) Color(0xFF0B0716) else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniHeaderButton(
    icon: Int,
    description: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            aaiIcon(icon),
            contentDescription = description,
            tint = TextMuted,
            modifier = Modifier.size(15.dp)
        )
    }
}

@Composable
private fun CompactMessage(message: MessageEntity) {
    when (message.role) {
        "user" -> Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp, 12.dp, 3.dp, 12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    message.content,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        "error" -> Text(message.content, fontSize = 11.sp, color = AccentRed)
        else -> Box(
            modifier = Modifier
                .clip(RoundedCornerShape(3.dp, 12.dp, 12.dp, 12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            MarkdownText(message.content)
        }
    }
}