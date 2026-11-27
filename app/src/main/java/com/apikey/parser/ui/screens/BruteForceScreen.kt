package com.apikey.parser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apikey.parser.vm.AppViewModel

@Composable
fun BruteForceScreen(vm: AppViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    val selected = remember { mutableStateListOf<String>().apply { addAll(state.providers.map { it.id }) } }
    var charset by remember { mutableStateOf("abcdefghijklmnopqrstuvwxyz0123456789") }
    var length by remember { mutableStateOf("4") }
    var progress by remember { mutableStateOf(0 to 0) }
    var current by remember { mutableStateOf("") }
    var found by remember { mutableStateOf(listOf<String>()) }
    var running by remember { mutableStateOf(false) }
    Column(Modifier.padding(16.dp)) {
        Text("Подбор ключей по всем провайдерам", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("Провайдеры:")
        state.providers.forEach { p ->
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = p.id in selected, onCheckedChange = { c ->
                    if (c) selected.add(p.id) else selected.remove(p.id)
                })
                Text(p.name)
            }
        }
        OutlinedTextField(value = charset, onValueChange = { charset = it }, label = { Text("Набор символов") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = length, onValueChange = { length = it }, label = { Text("Длина") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text("Текущий: $current  Прогресс: ${progress.first}/${progress.second}")
        LinearProgressIndicator(progress = if (progress.second > 0) progress.first.toFloat() / progress.second else 0f, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text("Найдено: ${found.size}")
        found.takeLast(20).forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
        Spacer(Modifier.height(12.dp))
        Row {
            Button(enabled = !running && selected.isNotEmpty(), onClick = {
                running = true
                found = emptyList()
                progress = 0 to 0
                vm.bruteForce(selected.toList(), charset, length.toIntOrNull() ?: 4,
                    onFound = { key, _ -> found = found + key },
                    onProgress = { a, b, name -> progress = a to b; current = name },
                    onDone = { running = false }
                )
            }) { Text("Старт") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onBack) { Text("Назад") }
        }
    }
}
