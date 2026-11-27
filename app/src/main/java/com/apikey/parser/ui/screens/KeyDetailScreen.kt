package com.apikey.parser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.apikey.parser.data.model.ApiKey
import com.apikey.parser.vm.AppViewModel

@Composable
fun KeyDetailScreen(vm: AppViewModel, keyId: String, onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    val k = state.keys.find { it.id == keyId } ?: return
    val p = state.providers.find { it.id == k.providerId }
    val clip = LocalClipboardManager.current
    Column(Modifier.padding(16.dp)) {
        Text("Ключ", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("Провайдер: ${p?.name ?: "?"}")
        Text("Ключ: ${k.key}")
        Text("Статус: ${k.status}")
        Text("Баланс: ${k.balance ?: "-"}")
        Text("Проверен: ${if (k.lastChecked == 0L) "-" else java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(java.util.Date(k.lastChecked))}")
        Text("Истекает: ${k.expiresAt?.let { java.text.SimpleDateFormat("dd.MM.yyyy").format(java.util.Date(it)) } ?: "-"}")
        Spacer(Modifier.height(8.dp))
        Text("Модели:", style = MaterialTheme.typography.titleMedium)
        k.models.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
        Spacer(Modifier.height(12.dp))
        Row {
            Button(onClick = { vm.validateKey(k.id) }) { Text("Проверить") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { clip.setText(AnnotatedString(k.key)) }) { Text("Копировать") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { vm.toggleFavorite(k.id) }) { Text(if (k.favorite) "★" else "☆") }
        }
        Spacer(Modifier.height(8.dp))
        Row {
            OutlinedButton(onClick = { vm.removeKey(k.id); onBack() }) { Text("Удалить") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onBack) { Text("Назад") }
        }
    }
}
