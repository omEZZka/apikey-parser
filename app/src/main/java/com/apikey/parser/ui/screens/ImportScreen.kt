package com.apikey.parser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apikey.parser.vm.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(vm: AppViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    val clipboard = LocalClipboardManager.current
    var text by remember { mutableStateOf("") }
    var providerId by remember { mutableStateOf(state.providers.firstOrNull()?.id ?: "") }
    var report by remember { mutableStateOf<AppViewModel.SmartImportReport?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Умный импорт") }, navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } }) }
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Вставьте текст, JSON, конфиг или список ключей — парсер сам найдёт провайдеров и ключи.",
                style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    clipboard.getText()?.text?.let { text = it }
                }) { Icon(Icons.Default.ContentPaste, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Вставить") }
                OutlinedButton(onClick = { text = ""; report = null }) { Text("Очистить") }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Данные") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp)
            )
            Spacer(Modifier.height(12.dp))
            Button(
                enabled = text.isNotBlank(),
                onClick = { report = vm.smartImport(text) },
                modifier = Modifier.fillMaxWidth()
            ) { Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Распознать и добавить") }

            report?.let { r ->
                Spacer(Modifier.height(16.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Результат", fontWeight = FontWeight.SemiBold)
                        Text("Провайдеров создано: ${r.providersCreated}")
                        Text("Ключей добавлено: ${r.keysAdded}")
                        Text("Дубликатов: ${r.keysDuplicate}")
                        if (r.providers.isNotEmpty()) Text("Найдены: ${r.providers.joinToString()}")
                    }
                }
            }

            if (state.providers.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Text("Или вручную для провайдера", style = MaterialTheme.typography.titleSmall)
                state.providers.forEach { p ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(selected = p.id == providerId, onClick = { providerId = p.id })
                        Text(p.name)
                    }
                }
                Button(
                    enabled = providerId.isNotBlank() && text.isNotBlank(),
                    onClick = { vm.importKeys(providerId, text); onBack() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Добавить только этому провайдеру") }
            }
        }
    }
}
