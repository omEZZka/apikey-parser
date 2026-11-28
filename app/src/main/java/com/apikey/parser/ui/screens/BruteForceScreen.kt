package com.apikey.parser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apikey.parser.vm.AppViewModel

private data class GenStats(val generated: Long = 0, val checked: Long = 0, val found: Long = 0, val rate: Double = 0.0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BruteForceScreen(vm: AppViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    val selected = remember { mutableStateListOf<String>().apply { addAll(state.providers.map { it.id }) } }
    var mode by remember { mutableStateOf(0) }
    var charset by remember { mutableStateOf("abcdefghijklmnopqrstuvwxyz0123456789") }
    var length by remember { mutableStateOf("14") }
    var prefix by remember { mutableStateOf("") }
    var dict by remember { mutableStateOf("") }
    var examples by remember { mutableStateOf("") }
    var customCharset by remember { mutableStateOf(false) }
    var target by remember { mutableStateOf("0") }
    var progress by remember { mutableStateOf(0 to 0) }
    var current by remember { mutableStateOf("") }
    var found by remember { mutableStateOf(listOf<String>()) }
    var attempts by remember { mutableStateOf(listOf<String>()) }
    var stats by remember { mutableStateOf(GenStats()) }
    var running by remember { mutableStateOf(false) }

    val exampleLines = remember(examples) { examples.lines().map { it.trim() }.filter { it.isNotBlank() } }
    val analysis = remember(examples, selected.toList()) {
        if (exampleLines.size >= 2) vm.analyzeExamples(examples)
        else vm.analyzeExamples(vm.savedExamples(selected.toList()).joinToString("\n"))
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Подбор ключей") }, navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } }) }
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState())) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(selected = mode == 0, onClick = { mode = 0 }, shape = SegmentedButtonDefaults.itemShape(0, 3)) { Text("Генератор") }
                SegmentedButton(selected = mode == 1, onClick = { mode = 1 }, shape = SegmentedButtonDefaults.itemShape(1, 3)) { Text("Словарь") }
                SegmentedButton(selected = mode == 2, onClick = { mode = 2 }, shape = SegmentedButtonDefaults.itemShape(2, 3)) { Text("Перебор") }
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
                    Text("Умный генератор", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Анализирует структуру ваших ключей и создаёт новые случайные: мутации, перестановки сегментов, base64/hex, словарь и марковский выбор символов.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { examples = vm.savedExamples(selected.toList()).joinToString("\n") }, modifier = Modifier.weight(1f)) {
                            Text("Сохранённые")
                        }
                        OutlinedButton(onClick = { examples = "" }, modifier = Modifier.weight(1f)) { Text("Очистить") }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = examples, onValueChange = { examples = it },
                        label = { Text("Примеры ключей (по одному в строке)") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp, max = 180.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    if (analysis != null) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Найденная структура", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(analysis.template, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(
                                    "Сегментов: ${analysis.segments.size} · переменных позиций: ${analysis.variablePositions().size} · пространство ≈ ${fmt(analysis.space())}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "Новые ключи не перебираются по порядку — они генерируются случайно с учётом этой структуры.",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    } else {
                        Text("Добавьте 2+ ключа (или вставьте примеры), чтобы генератор выучил структуру. Иначе он генерирует случайные base64-строки.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = customCharset, onCheckedChange = { customCharset = it })
                        Text("Задать алфавит вручную", style = MaterialTheme.typography.bodySmall)
                    }
                    if (customCharset) {
                        OutlinedTextField(value = charset, onValueChange = { charset = it }, label = { Text("Алфавит") }, modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text("Цель проверок (0 — без остановки)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatBox("Создано", stats.generated.toString(), Modifier.weight(1f))
                        StatBox("Проверено", stats.checked.toString(), Modifier.weight(1f))
                        StatBox("Найдено", stats.found.toString(), Modifier.weight(1f))
                        StatBox("Key/с", stats.rate.toInt().toString(), Modifier.weight(1f))
                    }
                }
                1 -> OutlinedTextField(value = dict, onValueChange = { dict = it }, label = { Text("Кандидаты (по одному в строке)") }, modifier = Modifier.fillMaxWidth().height(200.dp))
                else -> {
                    OutlinedTextField(value = prefix, onValueChange = { prefix = it }, label = { Text("Известный префикс") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = charset, onValueChange = { charset = it }, label = { Text("Набор символов") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = length, onValueChange = { length = it }, label = { Text("Длина случайной части") }, modifier = Modifier.fillMaxWidth())
                    Text("Комбинаций: ${estimate(charset, length)}", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(10.dp))
            Text("Параллелизм: ${state.concurrency}", style = MaterialTheme.typography.bodySmall)
            Slider(value = state.concurrency.toFloat(), onValueChange = { vm.setConcurrency(it.toInt()) }, valueRange = 1f..32f, steps = 30)
            Spacer(Modifier.height(4.dp))
            if (running && progress.second == 0) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(progress = if (progress.second > 0) (progress.first.toFloat() / progress.second).coerceIn(0f, 1f) else 0f, modifier = Modifier.fillMaxWidth())
            }
            Text("Прогресс: ${progress.first}${if (progress.second > 0) "/${progress.second}" else ""}  $current", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutputColumn("Удачные (${found.size})", found, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                OutputColumn("Перебрано (${stats.checked})", attempts, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    enabled = !running && selected.isNotEmpty(),
                    onClick = {
                        running = true; found = emptyList(); attempts = emptyList(); progress = 0 to 0
                        stats = GenStats()
                        val attemptCb: (String) -> Unit = { k -> attempts = (attempts + k).takeLast(120) }
                        val foundCb: (String, String) -> Unit = { k, _ -> found = found + k }
                        val progressCb: (Int, Int, String) -> Unit = { a, b, n -> progress = a to b; current = n }
                        when (mode) {
                            0 -> vm.generateKeys(
                                providerIds = selected.toList(),
                                examples = exampleLines,
                                charset = charset,
                                customCharset = customCharset,
                                target = target.toIntOrNull() ?: 0,
                                onFound = foundCb,
                                onProgress = progressCb,
                                onAttempt = attemptCb,
                                onStats = { g, c, f, r -> stats = GenStats(g, c, f, r) },
                                onDone = { running = false }
                            )
                            1 -> vm.testCandidates(selected.toList(), dict.lines().map { it.trim() }.filter { it.isNotBlank() },
                                onFound = foundCb, onProgress = progressCb, onAttempt = attemptCb,
                                onDone = { running = false; stats = stats.copy(checked = progress.first.toLong(), found = found.size.toLong()) })
                            else -> vm.bruteForce(selected.toList(), charset, length.toIntOrNull() ?: 4, prefix,
                                onFound = foundCb, onProgress = progressCb, onAttempt = attemptCb,
                                onDone = { running = false; stats = stats.copy(checked = progress.first.toLong(), found = found.size.toLong()) })
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(if (running) "Работает..." else "Старт") }
                if (mode == 0) {
                    OutlinedButton(onClick = { vm.stopGeneration(); running = false }, enabled = running, modifier = Modifier.weight(1f)) {
                        Text("Стоп")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
        Column(Modifier.padding(8.dp)) {
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

private fun fmt(n: Double): String = when {
    n >= 1e12 -> String.format("%.1e", n)
    n >= 1e6 -> "${(n / 1e6).toInt()}M"
    else -> n.toLong().toString()
}

private fun estimate(charset: String, length: String): String {
    val l = length.toIntOrNull() ?: return "?"
    val n = Math.pow(charset.length.toDouble(), l.toDouble())
    return if (n.isInfinite() || n > 200000) ">200000 (лимит 200000)" else n.toLong().toString()
}
