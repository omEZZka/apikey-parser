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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apikey.parser.data.model.Provider
import com.apikey.parser.vm.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatternDialog(vm: AppViewModel, provider: Provider, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var text by remember { mutableStateOf("") }
    var analyzeOnSave by remember { mutableStateOf(true) }

    val enteredKeys = remember(text) {
        text.lines().map { it.trim() }.filter { it.isNotBlank() }
    }
    val pattern = remember(text) { vm.patternForKeys(enteredKeys) }
    val existing = vm.state.value.keys.count { it.providerId == provider.id }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ключи: ${provider.name}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("Вставьте все ключи этого провайдера (по одному в строке). Парсер найдёт общую маску для подбора.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { clipboard.getText()?.text?.let { text = it } }) {
                        Icon(Icons.Default.ContentPaste, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Вставить")
                    }
                    OutlinedButton(onClick = { text = "" }) { Text("Очистить") }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    label = { Text("Ключи") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 260.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text("Введено ключей: ${enteredKeys.size} · уже сохранено: $existing", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                if (enteredKeys.size >= 2) {
                    if (pattern != null) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Найден паттерн", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(pattern, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                val fixed = pattern.count { it != '?' }
                                Text("Фиксированных позиций: $fixed · переменных: ${pattern.length - fixed}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    } else {
                        Text("Ключи разной длины — нужны ключи одинаковой длины для маски.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Text("Добавьте минимум 2 ключа для поиска паттерна.", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = analyzeOnSave, onCheckedChange = { analyzeOnSave = it })
                    Text("Сохранить ключи", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = enteredKeys.isNotEmpty(),
                onClick = {
                    if (analyzeOnSave) enteredKeys.forEach { vm.addKey(provider.id, it, "pattern-input") }
                    onDismiss()
                }
            ) { Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
