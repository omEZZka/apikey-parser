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
    var attempts by remember { mutableStateOf(listOf<String>()) }
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
            Spacer(Modifier.height(10.dp))
            Text("Параллелизм: ${state.concurrency}", style = MaterialTheme.typography.bodySmall)
            Slider(value = state.concurrency.toFloat(), onValueChange = { vm.setConcurrency(it.toInt()) }, valueRange = 1f..32f, steps = 30)
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(progress = if (progress.second > 0) progress.first.toFloat() / progress.second else 0f, modifier = Modifier.fillMaxWidth())
            Text("Прогресс: ${progress.first}/${progress.second}  $current", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutputColumn("Удачные (${found.size})", found, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                OutputColumn("Перебрано (${progress.first})", attempts, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Button(
                enabled = !running && selected.isNotEmpty(),
                onClick = {
                    running = true; found = emptyList(); attempts = emptyList(); progress = 0 to 0
                    val attemptCb: (String) -> Unit = { k -> attempts = (attempts + k).takeLast(120) }
                    when (mode) {
                        0 -> vm.bruteForce(selected.toList(), charset, length.toIntOrNull() ?: 4, prefix,
                            onFound = { k, _ -> found = found + k },
                            onProgress = { a, b, n -> progress = a to b; current = n },
                            onAttempt = attemptCb,
                            onDone = { running = false })
                        1 -> vm.testCandidates(selected.toList(), dict.lines().map { it.trim() }.filter { it.isNotBlank() },
                            onFound = { k, _ -> found = found + k },
                            onProgress = { a, b, n -> progress = a to b; current = n },
                            onAttempt = attemptCb,
                            onDone = { running = false })
                        else -> vm.bruteForcePattern(selected.toList(),
                            onFound = { k, _ -> found = found + k },
                            onProgress = { a, b, n -> progress = a to b; current = n },
                            onAttempt = attemptCb,
                            onDone = { running = false })
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (running) "Работает..." else "Старт") }
        }
    }
}

@Composable
private fun OutputColumn(title: String, items: List<String>, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = color)
        Spacer(Modifier.height(4.dp))
        Surface(Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 220.dp), color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
            Column(Modifier.padding(6.dp).verticalScroll(rememberScrollState())) {
                if (items.isEmpty()) {
                    Text("—", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    items.asReversed().forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}

private fun estimate(charset: String, length: String): String {
    val l = length.toIntOrNull() ?: return "?"
    val n = Math.pow(charset.length.toDouble(), l.toDouble())
    return if (n.isInfinite() || n > 200000) ">200000 (лимит 200000)" else n.toLong().toString()
}
