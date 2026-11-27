package com.apikey.parser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apikey.parser.vm.AppViewModel

@Composable
fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit, onThemes: () -> Unit) {
    val state by vm.state.collectAsState()
    Column(Modifier.padding(16.dp)) {
        Text("Настройки", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Text("Язык: ${state.language}")
        Row {
            Button(onClick = { vm.setLanguage("ru") }) { Text("RU") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { vm.setLanguage("en") }) { Text("EN") }
        }
        Spacer(Modifier.height(8.dp))
        Text("Порог баланса: ${state.balanceThreshold}")
        Spacer(Modifier.height(8.dp))
        Text("Параллелизм: ${state.concurrency}")
        Spacer(Modifier.height(8.dp))
        Button(onClick = { vm.validateAll() }) { Text("Проверить все ключи") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onThemes) { Text("Темы") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBack) { Text("Назад") }
    }
}
