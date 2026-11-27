package com.apikey.parser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apikey.parser.vm.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit, onThemes: () -> Unit) {
    val state by vm.state.collectAsState()
    Scaffold(
        topBar = { TopAppBar(title = { Text("Настройки") }, navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } }) }
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Язык", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.language == "ru", onClick = { vm.setLanguage("ru") }, label = { Text("Русский") })
                FilterChip(selected = state.language == "en", onClick = { vm.setLanguage("en") }, label = { Text("English") })
            }
            Spacer(Modifier.height(16.dp))
            Text("Параллелизм запросов: ${state.concurrency}", style = MaterialTheme.typography.titleSmall)
            Slider(
                value = state.concurrency.toFloat(),
                onValueChange = { vm.setConcurrency(it.toInt()) },
                valueRange = 1f..32f,
                steps = 30
            )
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            SettingsRow("Темы", "32 схемы", onThemes)
            SettingsRow("Проверить все ключи", "запустить валидацию", { vm.validateAll() })
            Spacer(Modifier.height(16.dp))
            Text("Безопасность", style = MaterialTheme.typography.titleSmall)
            Text("Ключи хранятся в EncryptedSharedPreferences (AES-256).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            Text("API Key Parser 1.1.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.padding(16.dp)) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", style = MaterialTheme.typography.titleLarge)
        }
    }
    Spacer(Modifier.height(8.dp))
}
