package com.apikey.parser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apikey.parser.vm.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BruteForceScreen(vm: AppViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    val selected = remember { mutableStateListOf<String>().apply { addAll(state.providers.map { it.id }) } }
    var mode by remember { mutableStateOf(0) }
    var charset by remember { mutableStateOf("abcdefghijklmnopqrstuvwxyz0123456789") }
    var length by remember { mutableStateOf("4") }
    var prefix by remember { mutableStateOf("") }
    var dict by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf(0 to 0) }
    var current by remember { mutableStateOf("") }
    var found by remember { mutableStateOf(listOf<String>()) }
    var running by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Подбор ключей") }, navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } }) }
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState())) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(selected = mode == 0, onClick = { mode = 0 }, shape = SegmentedButtonDefaults.itemShape(0, 3)) { Text("Маска") }
                SegmentedButton(selected = mode == 1, onClick = { mode = 1 }, shape = SegmentedButtonDefaults.itemShape(1, 3)) { Text("Словарь") }
                SegmentedButton(selected = mode == 2, onClick = { mode = 2 }, shape = SegmentedButtonDefaults.itemShape(2, 3)) { Text("Паттерн") }
            }
            Spacer(Modifier.height(12.dp))
            Text("Провайдеры", style = MaterialTheme.typography.titleSmall)
            state.providers.forEach { p ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = p.id in selected, onCheckedChange = { c -> if (c) selected.add(p.id) else selected.remove(p.id) })
                    Text(p.name)
                    Spacer(Modifier.weight(1f))
                    val keys = state.keys.count { it.providerId == p.id }
                    Text("$keys ключей", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            when (mode) {
                0 -> {
                    OutlinedTextField(value = prefix, onValueChange = { prefix = it }, label = { Text("Известный префикс") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = charset, onValueChange = { charset = it }, label = { Text("Набор символов") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = length, onValueChange = { length = it }, label = { Text("Длина случайной части") }, modifier = Modifier.fillMaxWidth())
                    Text("Комбинаций: ${estimate(charset, length)}", style = MaterialTheme.typography.bodySmall)
                }
                1 -> OutlinedTextField(value = dict, onValueChange = { dict = it }, label = { Text("Кандидаты (по одному в строке)") }, modifier = Modifier.fillMaxWidth().height(200.dp))
                else -> {
                    Text("Паттерн строится из 2+ ключей одинаковой длины у провайдера. ? заменяет переменные позиции.", style = MaterialTheme.typography.bodySmall)
                    state.providers.filter { it.id in selected }.forEach { p ->
                        val pat = vm.detectPattern(p.id)
                        Text("${p.name}: ${pat ?: "нужно ≥2 ключей одинаковой длины"}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(progress = if (progress.second > 0) progress.first.toFloat() / progress.second else 0f, modifier = Modifier.fillMaxWidth())
            Text("Прогресс: ${progress.first}/${progress.second}  $current", style = MaterialTheme.typography.bodySmall)
            Text("Найдено: ${found.size}", style = MaterialTheme.typography.bodySmall)
            found.takeLast(10).forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.height(12.dp))
            Button(
                enabled = !running && selected.isNotEmpty(),
                onClick = {
                    running = true; found = emptyList(); progress = 0 to 0
                    when (mode) {
                        0 -> vm.bruteForce(selected.toList(), charset, length.toIntOrNull() ?: 4, prefix,
                            onFound = { k, _ -> found = found + k },
                            onProgress = { a, b, n -> progress = a to b; current = n },
                            onDone = { running = false })
                        1 -> vm.testCandidates(selected.toList(), dict.lines().map { it.trim() }.filter { it.isNotBlank() },
                            onFound = { k, _ -> found = found + k },
                            onProgress = { a, b, n -> progress = a to b; current = n },
                            onDone = { running = false })
                        else -> vm.bruteForcePattern(selected.toList(),
                            onFound = { k, _ -> found = found + k },
                            onProgress = { a, b, n -> progress = a to b; current = n },
                            onDone = { running = false })
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (running) "Работает..." else "Старт") }
        }
    }
}

private fun estimate(charset: String, length: String): String {
    val l = length.toIntOrNull() ?: return "?"
    val n = Math.pow(charset.length.toDouble(), l.toDouble())
    return if (n.isInfinite() || n > 200000) ">200000 (лимит 200000)" else n.toLong().toString()
}
