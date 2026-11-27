package com.apikey.parser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apikey.parser.vm.AppViewModel

@Composable
fun BruteForceScreen(vm: AppViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    val selected = remember { mutableStateListOf<String>().apply { addAll(state.providers.map { it.id }) } }
    var mode by remember { mutableStateOf(0) } // 0 random, 1 dict
    var charset by remember { mutableStateOf("abcdefghijklmnopqrstuvwxyz0123456789") }
    var length by remember { mutableStateOf("4") }
    var prefix by remember { mutableStateOf("") }
    var dict by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf(0 to 0) }
    var current by remember { mutableStateOf("") }
    var found by remember { mutableStateOf(listOf<String>()) }
    var running by remember { mutableStateOf(false) }
    Column(Modifier.padding(16.dp)) {
        Text("Подбор/проверка ключей", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Row {
            FilterChip(selected = mode == 0, onClick = { mode = 0 }, label = { Text("Случайный") })
            Spacer(Modifier.width(8.dp))
            FilterChip(selected = mode == 1, onClick = { mode = 1 }, label = { Text("Словарь") })
            Spacer(Modifier.width(8.dp))
            FilterChip(selected = mode == 2, onClick = { mode = 2 }, label = { Text("Паттерн") })
        }
        Spacer(Modifier.height(8.dp))
        Text("Провайдеры:")
        state.providers.forEach { p ->
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = p.id in selected, onCheckedChange = { c -> if (c) selected.add(p.id) else selected.remove(p.id) })
                Text(p.name)
            }
        }
        if (mode == 0) {
            OutlinedTextField(value = prefix, onValueChange = { prefix = it }, label = { Text("Префикс (известная часть)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = charset, onValueChange = { charset = it }, label = { Text("Набор символов") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = length, onValueChange = { length = it }, label = { Text("Длина подбираемой части") }, modifier = Modifier.fillMaxWidth())
        } else {
            OutlinedTextField(value = dict, onValueChange = { dict = it }, label = { Text("Ключи (по одному в строке)") }, modifier = Modifier.fillMaxWidth().height(200.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text("Прогресс: ${progress.first}/${progress.second}  $current")
        LinearProgressIndicator(progress = if (progress.second > 0) progress.first.toFloat() / progress.second else 0f, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text("Найдено: ${found.size}")
        found.takeLast(20).forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
        if (mode == 2) {
            state.providers.filter { it.id in selected }.forEach { p ->
                val pat = vm.detectPattern(p.id)
                Text("${p.name}: ${pat ?: "нужно >=2 ключей одинаковой длины"}", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(12.dp))
        Row {
            Button(enabled = !running && selected.isNotEmpty(), onClick = {
                running = true
                found = emptyList()
                progress = 0 to 0
                if (mode == 0) {
                    vm.bruteForce(selected.toList(), charset, length.toIntOrNull() ?: 4, prefix,
                        onFound = { key, _ -> found = found + key },
                        onProgress = { a, b, name -> progress = a to b; current = name },
                        onDone = { running = false }
                    )
                } else if (mode == 1) {
                    val cands = dict.lines().map { it.trim() }.filter { it.isNotBlank() }
                    vm.testCandidates(selected.toList(), cands,
                        onFound = { key, _ -> found = found + key },
                        onProgress = { a, b, name -> progress = a to b; current = name },
                        onDone = { running = false }
                    )
                } else {
                    vm.bruteForcePattern(selected.toList(),
                        onFound = { key, _ -> found = found + key },
                        onProgress = { a, b, name -> progress = a to b; current = name },
                        onDone = { running = false }
                    )
                }
            }) { Text("Старт") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onBack) { Text("Назад") }
        }
    }
}
