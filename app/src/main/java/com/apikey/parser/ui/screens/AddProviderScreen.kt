package com.apikey.parser.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apikey.parser.data.model.ProviderCatalog
import com.apikey.parser.vm.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProviderScreen(vm: AppViewModel, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var example by remember { mutableStateOf("") }
    var showPresets by remember { mutableStateOf(true) }
    val detected = remember(url) { ProviderCatalog.detectByUrl(url) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новый провайдер") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Назад") } }
            )
        }
    ) { pad ->
        LazyColumn(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text("Быстрый выбор", style = MaterialTheme.typography.titleMedium)
            }
            items(ProviderCatalog.profiles) { p ->
                Card(
                    Modifier.fillMaxWidth().clickable {
                        name = p.name
                        url = p.baseUrl
                        example = p.keyRegex
                        showPresets = false
                    }
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(p.name, fontWeight = FontWeight.SemiBold)
                        Text(p.baseUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text("Или вручную", style = MaterialTheme.typography.titleMedium)
            }
            item {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Имя") }, modifier = Modifier.fillMaxWidth())
            }
            item {
                OutlinedTextField(
                    value = url, onValueChange = { url = it },
                    label = { Text("Base URL") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    supportingText = { Text(detected?.let { "Определён: ${it.name}" } ?: "Например https://api.openai.com") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(value = example, onValueChange = { example = it }, label = { Text("Пример/шаблон ключа (необязательно)") }, modifier = Modifier.fillMaxWidth())
            }
            item {
                Button(
                    enabled = name.isNotBlank() || url.isNotBlank(),
                    onClick = {
                        vm.addProvider(name.ifBlank { detected?.name ?: url }, url)
                        if (example.isNotBlank()) {
                            vm.state.value.providers.lastOrNull()?.let { p ->
                                vm.updateProvider(p.id) { it.copy(keyExample = example, keyTemplate = example) }
                            }
                        }
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Сохранить") }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}
