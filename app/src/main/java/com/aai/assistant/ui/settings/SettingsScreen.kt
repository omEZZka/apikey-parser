package com.aai.assistant.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aai.assistant.ui.components.aaiIcon
import com.aai.assistant.ui.components.AaiIcons
import com.aai.assistant.data.prefs.AgentSettings
import com.aai.assistant.tools.ScreenBridge
import com.aai.assistant.ui.theme.AccentAmber
import com.aai.assistant.ui.theme.AccentGreen
import com.aai.assistant.ui.theme.AccentRed
import com.aai.assistant.ui.theme.AgentGradient
import com.aai.assistant.ui.theme.OutlineDim
import com.aai.assistant.ui.theme.TextMuted
import com.aai.assistant.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onUpdate: ((AgentSettings) -> AgentSettings) -> Unit,
    onSave: () -> Unit,
    onLoadModels: () -> Unit,
    onPickModel: (String) -> Unit,
    onPreset: (ProviderPreset) -> Unit,
    onTest: () -> Unit,
    onResetPrompt: () -> Unit
) {
    val context = LocalContext.current
    val s = state.settings
    var keyVisible by rememberSaveable { mutableStateOf(false) }
    var modelFilter by rememberSaveable { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = { Text("Настройки", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(aaiIcon(AaiIcons.Back), contentDescription = "Назад")
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(50))
                            .background(AgentGradient)
                            .clickable(onClick = onSave)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            aaiIcon(if (state.saved) AaiIcons.Check else AaiIcons.Save),
                            contentDescription = null,
                            tint = Color(0xFF07090F),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (state.saved) "Готово" else "Сохранить",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF07090F)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionCard(title = "Провайдер", subtitle = "OpenAI-совместимый API") {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    providerPresets.forEach { preset ->
                        val active = s.baseUrl.trimEnd('/') == preset.baseUrl
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (active) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    1.dp,
                                    if (active) MaterialTheme.colorScheme.primary else OutlineDim,
                                    RoundedCornerShape(50)
                                )
                                .clickable { onPreset(preset) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                preset.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                                else TextSecondary
                            )
                        }
                    }
                }
                Field(
                    value = s.baseUrl,
                    onChange = { v -> onUpdate { it.copy(baseUrl = v) } },
                    label = "Base URL",
                    placeholder = "https://api.openai.com/v1"
                )
                OutlinedTextField(
                    value = s.apiKey,
                    onValueChange = { v -> onUpdate { it.copy(apiKey = v) } },
                    label = { Text("API Key") },
                    leadingIcon = {
                        Icon(aaiIcon(AaiIcons.Key), contentDescription = null, tint = TextMuted)
                    },
                    trailingIcon = {
                        IconButton(onClick = { keyVisible = !keyVisible }) {
                            Icon(
                                aaiIcon(if (keyVisible) AaiIcons.VisibilityOff else AaiIcons.Visibility),
                                contentDescription = "Показать ключ",
                                tint = TextMuted
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (keyVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    colors = fieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(enabled = !state.testing, onClick = onTest)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.testing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = AccentAmber
                        )
                    } else {
                        Icon(aaiIcon(AaiIcons.Bolt),
                            contentDescription = null,
                            tint = AccentAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Проверить соединение",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                state.testResult?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.testOk) AccentGreen else AccentRed
                    )
                }
            }

            SectionCard(title = "Модель", subtitle = "Ручной ввод или список с сервера") {
                Field(
                    value = s.model,
                    onChange = { v -> onUpdate { it.copy(model = v) } },
                    label = "Model ID",
                    placeholder = "gpt-4o-mini"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(enabled = !state.loadingModels, onClick = onLoadModels)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.loadingModels) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(aaiIcon(AaiIcons.Refresh),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Загрузить модели (GET /models)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                state.modelsError?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = AccentRed)
                }
                if (state.models.isNotEmpty()) {
                    Field(
                        value = modelFilter,
                        onChange = { modelFilter = it },
                        label = "Поиск по ${state.models.size} моделям",
                        placeholder = "например: claude"
                    )
                    val filtered = state.models.filter {
                        modelFilter.isBlank() || it.id.contains(modelFilter, ignoreCase = true)
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .verticalScroll(rememberScrollState())
                            .padding(6.dp)
                    ) {
                        filtered.forEach { model ->
                            val active = model.id == s.model
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (active) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent
                                    )
                                    .clickable { onPickModel(model.id) }
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    model.id,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                    color = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                if (active) {
                                    Icon(aaiIcon(AaiIcons.Check),
                                        contentDescription = null,
                                        tint = AccentGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        if (filtered.isEmpty()) {
                            Text(
                                "Ничего не найдено",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            SectionCard(title = "Генерация", subtitle = "Параметры ответа модели") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Temperature",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "%.2f".format(s.temperature),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Slider(
                    value = s.temperature,
                    onValueChange = { v -> onUpdate { it.copy(temperature = v) } },
                    valueRange = 0f..2f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Field(
                        value = s.maxTokens.toString(),
                        onChange = { v ->
                            v.filter { it.isDigit() }.toIntOrNull()?.let { n ->
                                onUpdate { it.copy(maxTokens = n.coerceIn(1, 128000)) }
                            }
                        },
                        label = "max_tokens",
                        numeric = true,
                        modifier = Modifier.weight(1f)
                    )
                    Field(
                        value = s.maxIterations.toString(),
                        onChange = { v ->
                            v.filter { it.isDigit() }.toIntOrNull()?.let { n ->
                                onUpdate { it.copy(maxIterations = n.coerceIn(1, 100)) }
                            }
                        },
                        label = "шагов агента",
                        numeric = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            SectionCard(title = "Системный промт", subtitle = "Личность и правила агента") {
                OutlinedTextField(
                    value = s.systemPrompt,
                    onValueChange = { v -> onUpdate { it.copy(systemPrompt = v) } },
                    minLines = 4,
                    maxLines = 10,
                    colors = fieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "↺ Вернуть стандартный промт",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onResetPrompt)
                        .padding(vertical = 6.dp, horizontal = 4.dp)
                )
            }

            SectionCard(title = "Разрешения", subtitle = "Нужны для управления устройством") {
                PermissionRow(
                    icon = AaiIcons.Accessibility,
                    title = "Специальные возможности",
                    subtitle = if (ScreenBridge.isConnected()) "Подключено — агент видит экран"
                    else "Выключено — UI-инструменты не работают",
                    ok = ScreenBridge.isConnected()
                ) {
                    context.startActivity(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
                PermissionRow(
                    icon = AaiIcons.Layers,
                    title = "Поверх других приложений",
                    subtitle = "Плавающий мини-чат и кнопка ассистента",
                    ok = Settings.canDrawOverlays(context)
                ) {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
                PermissionRow(
                    icon = AaiIcons.Folder,
                    title = "Доступ ко всем файлам",
                    subtitle = "MANAGE_EXTERNAL_STORAGE для file_* инструментов",
                    ok = if (Build.VERSION.SDK_INT >= 30) Environment.isExternalStorageManager() else true
                ) {
                    if (Build.VERSION.SDK_INT >= 30) {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, OutlineDim, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
        content()
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = OutlineDim,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
    cursorColor = MaterialTheme.colorScheme.primary
)

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    numeric: Boolean = false,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 13.sp) },
        placeholder = if (placeholder.isNotEmpty()) {
            { Text(placeholder, fontSize = 13.sp, color = TextMuted) }
        } else null,
        singleLine = true,
        keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Number)
        else KeyboardOptions.Default,
        colors = fieldColors(),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun PermissionRow(
    icon: Int,
    title: String,
    subtitle: String,
    ok: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(
                    (if (ok) AccentGreen else AccentAmber).copy(alpha = 0.15f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                aaiIcon(icon),
                contentDescription = null,
                tint = if (ok) AccentGreen else AccentAmber,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background((if (ok) AccentGreen else AccentRed).copy(alpha = 0.15f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                if (ok) "ON" else "OFF",
                style = MaterialTheme.typography.labelSmall,
                color = if (ok) AccentGreen else AccentRed
            )
        }
    }
}