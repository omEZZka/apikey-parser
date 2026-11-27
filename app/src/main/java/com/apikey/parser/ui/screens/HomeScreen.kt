package com.apikey.parser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apikey.parser.data.model.ApiKey
import com.apikey.parser.data.model.Provider
import com.apikey.parser.vm.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: AppViewModel, onAddProvider: () -> Unit, onKey: (String) -> Unit, onImport: () -> Unit, onBrute: () -> Unit, onSettings: () -> Unit, onThemes: () -> Unit) {
    val state by vm.state.collectAsState()
    var query by remember { mutableStateOf("") }
    var onlyValid by remember { mutableStateOf(false) }
    var showSmart by remember { mutableStateOf(false) }

    val filteredKeys = state.keys.filter { k ->
        val p = state.providers.find { it.id == k.providerId }
        (!onlyValid || k.status == "valid") && (
            query.isBlank() || k.key.contains(query, true) || k.label.contains(query, true) ||
                p?.name?.contains(query, true) == true ||
                k.models.any { it.contains(query, true) }
            )
    }
    val validCount = state.keys.count { it.status == "valid" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API Key Parser", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, null) }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddProvider,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Провайдер") }
            )
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(12.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("Провайдеры", state.providers.size.toString(), Modifier.weight(1f))
                    StatCard("Ключи", state.keys.size.toString(), Modifier.weight(1f))
                    StatCard("Валидные", validCount.toString(), Modifier.weight(1f), highlight = validCount > 0)
                }
            }
            item {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Поиск по ключу, провайдеру, модели") },
                    leadingIcon = { Icon(Icons.Default.AutoAwesome, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            item {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onImport, modifier = Modifier.weight(1f)) { Text("Умный импорт") }
                    FilledTonalButton(onClick = onBrute, modifier = Modifier.weight(1f)) { Text("Подбор") }
                    OutlinedButton(onClick = { showSmart = true }, modifier = Modifier.weight(1f)) { Text("Ещё") }
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = onlyValid, onCheckedChange = { onlyValid = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Только валидные")
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { vm.validateAll() }) { Text("Проверить все") }
                }
            }
            if (state.providers.isEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth().padding(top = 24.dp)) {
                        Column(Modifier.padding(20.dp)) {
                            Text("Добавьте провайдера", style = MaterialTheme.typography.titleMedium)
                            Text("Например: OpenAI, OpenRouter, Groq, DeepSeek, Gemini или свой base URL.", style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = onAddProvider) { Text("Добавить провайдера") }
                        }
                    }
                }
            }
            items(state.providers) { p ->
                ProviderHeader(p, state.keys.count { it.providerId == p.id }, state.keys.count { it.providerId == p.id && it.status == "valid" })
            }
            items(filteredKeys) { k ->
                val p = state.providers.find { it.id == k.providerId }
                KeyCard(k, p?.name ?: "?", { vm.toggleFavorite(k.id) }, { onKey(k.id) })
            }
        }
    }

    if (showSmart) {
        AlertDialog(
            onDismissRequest = { showSmart = false },
            title = { Text("Действия") },
            text = { Text("Дополнительные функции") },
            confirmButton = {
                TextButton(onClick = { showSmart = false; onBrute() }) { Text("Подбор") }
            },
            dismissButton = {
                TextButton(onClick = { showSmart = false; onThemes() }) { Text("Темы") }
            }
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier, highlight: Boolean = false) {
    Card(modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ProviderHeader(p: Provider, count: Int, valid: Int) {
    Row(
        Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(
            if (valid > 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline
        ))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(p.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(p.baseUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("$valid/$count", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun KeyCard(k: ApiKey, providerName: String, onFav: () -> Unit, onClick: () -> Unit) {
    val statusColor = when (k.status) {
        "valid" -> Color(0xFF4CAF50)
        "invalid" -> MaterialTheme.colorScheme.error
        "checking" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outline
    }
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() }) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(statusColor))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(providerName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (k.favorite) { Spacer(Modifier.width(4.dp)); Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(14.dp)) }
                }
                Text(mask(k.key), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    buildString {
                        append(k.status)
                        if (!k.balance.isNullOrBlank()) append(" · \$").append(k.balance)
                        if (k.models.isNotEmpty()) append(" · ").append(k.models.size).append(" моделей")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onFav) { Icon(if (k.favorite) Icons.Default.Star else Icons.Default.StarBorder, null) }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

private fun mask(key: String): String = if (key.length <= 10) "••••" else key.take(6) + "••••••" + key.takeLast(4)
