package com.apikey.parser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apikey.parser.vm.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyDetailScreen(vm: AppViewModel, keyId: String, onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    val k = state.keys.find { it.id == keyId }
    val p = k?.let { key -> state.providers.find { it.id == key.providerId } }
    val clip = LocalClipboardManager.current
    var showDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(p?.name ?: "Ключ") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } },
                actions = {
                    IconButton(onClick = { k?.let { vm.toggleFavorite(it.id) } }) {
                        Icon(if (k?.favorite == true) Icons.Default.Star else Icons.Default.StarBorder, null)
                    }
                }
            )
        }
    ) { pad ->
        if (k == null) { Text("Ключ удалён", Modifier.padding(pad).padding(16.dp)); return@Scaffold }
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState())) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("API-ключ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(k.key, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { clip.setText(AnnotatedString(k.key)) }) {
                            Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Копировать")
                        }
                        OutlinedButton(onClick = { vm.validateKey(k.id) }) {
                            Icon(Icons.Default.Refresh, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Проверить")
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            InfoCard("Статус", k.status)
            InfoCard("Баланс", k.balance ?: "—")
            InfoCard("Моделей", k.models.size.toString())
            InfoCard("Проверен", if (k.lastChecked == 0L) "—" else java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(k.lastChecked)))
            Spacer(Modifier.height(12.dp))
            if (k.models.isNotEmpty()) {
                Text("Модели", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                k.models.forEach { m ->
                    Surface(Modifier.fillMaxWidth().padding(vertical = 2.dp), shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(m, Modifier.padding(8.dp), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = { showDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Delete, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Удалить ключ")
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Удалить ключ?") },
            text = { Text("Действие необратимо.") },
            confirmButton = { TextButton(onClick = { vm.removeKey(keyId); showDelete = false; onBack() }) { Text("Удалить") } },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun InfoCard(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
