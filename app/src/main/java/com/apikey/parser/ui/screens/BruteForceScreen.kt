package com.apikey.parser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apikey.parser.vm.AppViewModel

@Composable
fun BruteForceScreen(vm: AppViewModel, providerId: String, onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    val p = state.providers.find { it.id == providerId }
    var charset by remember { mutableStateOf("abcdefghijklmnopqrstuvwxyz0123456789") }
    var length by remember { mutableStateOf("4") }
    var progress by remember { mutableStateOf(0 to 0) }
    var found by remember { mutableStateOf(listOf<String>()) }
    var running by remember { mutableStateOf(false) }
    Column(Modifier.padding(16.dp)) {
        Text("Подбор ключей: ${p?.name ?: "?"}", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = charset, onValueChange = { charset = it }, label = { Text("Набор символов") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = length, onValueChange = { length = it }, label = { Text("Длина") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text("Прогресс: ${progress.first}/${progress.second}")
        LinearProgressIndicator(progress = if (progress.second > 0) progress.first.toFloat() / progress.second else 0f, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text("Найдено: ${found.size}")
        found.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
        Spacer(Modifier.height(12.dp))
        Row {
            Button(enabled = !running, onClick = {
                running = true
                found = emptyList()
                vm.bruteForce(providerId, charset, length.toIntOrNull() ?: 4,
                    onFound = { found = found + it },
                    onProgress = { a, b -> progress = a to b },
                    onDone = { running = false }
                )
            }) { Text("Старт") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onBack) { Text("Назад") }
        }
    }
}
