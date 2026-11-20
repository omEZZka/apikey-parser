package com.aai.assistant.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aai.assistant.tools.ScreenBridge
import com.aai.assistant.ui.theme.AccentGreen
import com.aai.assistant.ui.theme.AccentRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onUpdate: ((com.aai.assistant.data.prefs.AgentSettings) -> com.aai.assistant.data.prefs.AgentSettings) -> Unit,
    onSave: () -> Unit,
    onLoadModels: () -> Unit,
    onPickModel: (String) -> Unit
) {
    val context = LocalContext.current
    val s = state.settings
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitle("Провайдер API")
            OutlinedTextField(
                value = s.baseUrl,
                onValueChange = { v -> onUpdate { it.copy(baseUrl = v) } },
                label = { Text("Base URL") },
                placeholder = { Text("https://api.openai.com/v1") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = s.apiKey,
                onValueChange = { v -> onUpdate { it.copy(apiKey = v) } },
                label = { Text("API Key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = s.model,
                onValueChange = { v -> onUpdate { it.copy(model = v) } },
                label = { Text("Model ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(
                onClick = onLoadModels,
                enabled = !state.loadingModels,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.loadingModels) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp).width(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                }
                Spacer(Modifier.width(8.dp))
                Text("Загрузить список моделей (GET /models)")
            }
            state.modelsError?.let {
                Text(it, color = AccentRed, fontSize = 12.sp)
            }
            if (state.models.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp)
                    ) {
                        state.models.forEach { model ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPickModel(model.id) }
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    model.id,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f),
                                    color = if (model.id == s.model) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                if (model.id == s.model) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = AccentGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }

            SectionTitle("Генерация")
            OutlinedTextField(
                value = s.systemPrompt,
                onValueChange = { v -> onUpdate { it.copy(systemPrompt = v) } },
                label = { Text("Системный промт") },
                minLines = 3,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth()
            )
            Text("Temperature: ${"%.2f".format(s.temperature)}", fontSize = 13.sp)
            Slider(
                value = s.temperature,
                onValueChange = { v -> onUpdate { it.copy(temperature = v) } },
                valueRange = 0f..2f
            )
            OutlinedTextField(
                value = s.maxTokens.toString(),
                onValueChange = { v -> v.toIntOrNull()?.let { n -> onUpdate { it.copy(maxTokens = n.coerceIn(1, 128000)) } } },
                label = { Text("max_tokens") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = s.maxIterations.toString(),
                onValueChange = { v -> v.toIntOrNull()?.let { n -> onUpdate { it.copy(maxIterations = n.coerceIn(1, 100)) } } },
                label = { Text("Макс. шагов агента за ход") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.saved) "Сохранено ✓" else "Сохранить настройки")
            }

            SectionTitle("Разрешения устройства")
            PermissionRow(
                icon = Icons.Default.Accessibility,
                title = "Специальные возможности",
                subtitle = if (ScreenBridge.isConnected()) "Подключено — агент видит экран" else "Не подключено — нужно для управления UI",
                ok = ScreenBridge.isConnected()
            ) {
                context.startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            PermissionRow(
                icon = Icons.Default.Layers,
                title = "Поверх других приложений",
                subtitle = "Плавающая кнопка ассистента",
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
                icon = Icons.Default.Folder,
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
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun PermissionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    ok: Boolean,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp)
                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (ok) AccentGreen else AccentRed
            )
        }
    }
}