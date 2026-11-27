package com.apikey.parser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apikey.parser.data.model.ApiKey
import com.apikey.parser.data.model.Provider
import com.apikey.parser.vm.AppViewModel

@Composable
fun HomeScreen(vm: AppViewModel, onAddProvider: () -> Unit, onKey: (String) -> Unit, onBrute: (String) -> Unit, onSettings: () -> Unit, onThemes: () -> Unit) {
    val state by vm.state.collectAsState()
    var query by remember { mutableStateOf("") }
    val filteredKeys = state.keys.filter { k ->
        query.isBlank() || k.key.contains(query, true) || k.label.contains(query, true) ||
            state.providers.find { it.id == k.providerId }?.name?.contains(query, true) == true
    }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("API Key Parser") }, actions = {
                IconButton(onClick = onThemes) { Text("T") }
                IconButton(onClick = onSettings) { Text("S") }
            })
        },
        floatingActionButton = { FloatingActionButton(onClick = onAddProvider) { Icon(Icons.Default.Add, null) } }
    ) { pad ->
        Column(Modifier.padding(pad).padding(12.dp)) {
            OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Поиск") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            LazyColumn {
                items(state.providers) { p ->
                    ProviderHeader(p, state.keys.count { it.providerId == p.id }, { onBrute(p.id) })
                }
                items(filteredKeys) { k ->
                    val p = state.providers.find { it.id == k.providerId }
                    KeyCard(k, p?.name ?: "?", { vm.toggleFavorite(k.id) }, { onKey(k.id) })
                }
            }
        }
    }
}

@Composable
private fun ProviderHeader(p: Provider, count: Int, onBrute: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(p.name, style = MaterialTheme.typography.titleMedium)
            Text(p.baseUrl, style = MaterialTheme.typography.bodySmall)
        }
        Row {
            Text("keys: $count", modifier = Modifier.padding(end = 8.dp))
            TextButton(onClick = onBrute) { Text("Подбор") }
        }
    }
}

@Composable
private fun KeyCard(k: ApiKey, providerName: String, onFav: () -> Unit, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() }) {
        Column(Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(providerName, style = MaterialTheme.typography.labelMedium)
                IconButton(onClick = onFav) { Icon(if (k.favorite) Icons.Default.Star else Icons.Default.StarBorder, null) }
            }
            Text(mask(k.key), style = MaterialTheme.typography.bodyMedium)
            Text("Статус: ${k.status}  Баланс: ${k.balance ?: "-"}", style = MaterialTheme.typography.bodySmall)
            Text("Модели: ${k.models.size}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun mask(key: String): String = if (key.length <= 8) "****" else key.take(4) + "****" + key.takeLast(4)
