package com.aai.assistant.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aai.assistant.ui.components.aaiIcon
import com.aai.assistant.ui.components.AaiIcons
import com.aai.assistant.data.db.MessageEntity
import com.aai.assistant.data.media.ImageImporter
import com.aai.assistant.domain.ToolCall
import com.aai.assistant.ui.components.AgentAvatar
import com.aai.assistant.ui.components.MarkdownText
import com.aai.assistant.ui.components.SuggestionChips
import com.aai.assistant.ui.components.ToolCallCard
import com.aai.assistant.ui.theme.AccentAmber
import com.aai.assistant.ui.theme.BgElevated
import com.aai.assistant.ui.theme.DangerGradient
import com.aai.assistant.ui.theme.OutlineDim
import com.aai.assistant.ui.theme.SendGradient
import com.aai.assistant.ui.theme.TextMuted
import kotlinx.serialization.json.Json

private val toolCallsJson = Json { ignoreUnknownKeys = true }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatUiState,
    sessionTitle: String,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onSend: (String, List<String>) -> Unit,
    onStop: () -> Unit,
    onMinimize: () -> Unit,
    onContinue: () -> Unit = {}
) {
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var input by rememberSaveable { mutableStateOf("") }
    var attachedImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var importing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var pendingUris by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(4)
    ) { uris ->
        if (uris.isNotEmpty()) pendingUris = uris
    }
    LaunchedEffect(pendingUris) {
        if (pendingUris.isNotEmpty()) {
            importing = true
            val imported = ImageImporter.importAll(context, pendingUris)
            attachedImages = (attachedImages + imported).take(4)
            pendingUris = emptyList()
            importing = false
        }
    }

    LaunchedEffect(state.messages.size, state.messages.lastOrNull()?.content) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AgentAvatar(size = 30)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                sessionTitle,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (state.isRunning) {
                                Text(
                                    state.iterationInfo ?: "агент работает…",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AccentAmber
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(aaiIcon(AaiIcons.Menu), contentDescription = "Меню")
                    }
                },
                actions = {
                    IconButton(onClick = onMinimize) {
                        Icon(aaiIcon(AaiIcons.Pip),
                            contentDescription = "Свернуть в плавающий чат",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(aaiIcon(AaiIcons.Settings), contentDescription = "Настройки")
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .imePadding()
            ) {
                val lastIsStopped = !state.isRunning &&
                    state.messages.lastOrNull()?.let {
                        it.role == "error" && it.content.contains("остановлен")
                    } == true
                AnimatedVisibility(
                    visible = lastIsStopped,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(SendGradient)
                                .clickable(onClick = onContinue)
                                .padding(horizontal = 18.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                aaiIcon(AaiIcons.Play),
                                contentDescription = null,
                                tint = Color(0xFF0B0716),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Продолжить",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF0B0716)
                            )
                        }
                    }
                }
                AnimatedVisibility(
                    visible = state.isRunning,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(DangerGradient)
                                .clickable(onClick = onStop)
                                .padding(horizontal = 18.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(aaiIcon(AaiIcons.Stop),
                                contentDescription = null,
                                tint = Color(0xFF2B0B0B),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Остановить агента",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF2B0B0B)
                            )
                        }
                    }
                }
                if (state.messages.isEmpty()) {
                    SuggestionChips(
                        onPick = { prompt -> input = prompt },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                if (attachedImages.isNotEmpty() || importing) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        attachedImages.forEachIndexed { index, b64 ->
                            AttachedThumb(b64) {
                                attachedImages = attachedImages.filterIndexed { i, _ -> i != index }
                            }
                        }
                        if (importing) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 10.dp, top = 2.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, OutlineDim, RoundedCornerShape(26.dp))
                        .padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    IconButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .size(40.dp)
                    ) {
                        Icon(aaiIcon(AaiIcons.Attach),
                            contentDescription = "Прикрепить фото",
                            tint = if (attachedImages.isNotEmpty()) MaterialTheme.colorScheme.primary else TextMuted,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    TextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                "Задача для агента…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        },
                        maxLines = 5,
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
                            .padding(bottom = 4.dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (canSend) SendGradient
                                else androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.surfaceVariant)
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
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = AccentAmber
                            )
                        } else {
                            Icon(aaiIcon(AaiIcons.Send),
                                contentDescription = "Отправить",
                                tint = if (canSend) Color(0xFF0B0716) else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (state.messages.isEmpty()) {
            EmptyChatHint(Modifier.padding(padding))
        } else {
            val toolResults = state.messages
                .filter { it.role == "tool" }
                .associateBy { it.toolCallId }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.messages, key = { it.id }) { message ->
                    when (message.role) {
                        "user" -> UserBubble(message.content, message.imagesJson)
                        "assistant" -> if (message.content.isNotBlank()) {
                            AssistantBubble(message.content)
                        }
                        "assistant_calls" -> {
                            val calls = message.toolCallsJson?.let {
                                runCatching {
                                    toolCallsJson.decodeFromString(
                                        kotlinx.serialization.builtins.ListSerializer(ToolCall.serializer()),
                                        it
                                    )
                                }.getOrNull()
                            } ?: emptyList()
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                calls.forEach { call ->
                                    val result = toolResults[call.id]
                                    ToolCallCard(
                                        toolName = call.function.name,
                                        arguments = call.function.arguments,
                                        result = result?.content,
                                        isError = result?.isError ?: false
                                    )
                                }
                            }
                        }
                        "tool" -> Unit
                        "error" -> ErrorBubble(message.content)
                    }
                }
                if (state.isRunning) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(13.dp),
                                strokeWidth = 2.dp,
                                color = AccentAmber
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                state.iterationInfo ?: "Агент думает…",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyChatHint(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            AgentAvatar(size = 72)
            Spacer(Modifier.height(20.dp))
            Text(
                "Чем помочь?",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Я вижу экран, управляю приложениями, выполняю команды и работаю с файлами. Выбери подсказку внизу или напиши задачу.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun UserBubble(text: String, imagesJson: String?) {
    val images = remember(imagesJson) {
        imagesJson?.let {
            runCatching {
                kotlinx.serialization.json.Json.decodeFromString(
                    com.aai.assistant.domain.StringListSerializer,
                    it
                )
            }.getOrNull()
        } ?: emptyList()
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Column(horizontalAlignment = Alignment.End) {
            if (images.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    images.forEach { b64 -> MessageImage(b64, 140) }
                }
                if (text.isNotBlank()) Spacer(Modifier.height(4.dp))
            }
            if (text.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .clip(RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun MessageImage(base64: String, sizeDp: Int) {
    val bitmap by produceState<android.graphics.Bitmap?>(null, base64) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            ImageImporter.decode(base64)
        }
    }
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = "Фото",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, OutlineDim, RoundedCornerShape(14.dp))
        )
    }
}

@Composable
private fun AttachedThumb(base64: String, onRemove: () -> Unit) {
    Box {
        MessageImage(base64, 64)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color(0xCC07090F))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(aaiIcon(AaiIcons.Close),
                contentDescription = "Убрать фото",
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun AssistantBubble(text: String) {
    val context = LocalContext.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Column(modifier = Modifier.widthIn(max = 340.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AgentAvatar(size = 20)
                Spacer(Modifier.width(6.dp))
                Text(
                    "aAi",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    aaiIcon(AaiIcons.Copy),
                    contentDescription = "Копировать ответ",
                    tint = TextMuted,
                    modifier = Modifier
                        .size(14.dp)
                        .clickable {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                                as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("aai", text))
                        }
                )
            }
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp, 20.dp, 20.dp, 20.dp))
                    .background(BgElevated)
                    .border(1.dp, OutlineDim, RoundedCornerShape(6.dp, 20.dp, 20.dp, 20.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                if (text.isBlank()) {
                    Text("…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    MarkdownText(text)
                }
            }
        }
    }
}

@Composable
private fun ErrorBubble(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
    }
}