package com.apikey.parser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apikey.parser.vm.AppViewModel

@Composable
fun ImportScreen(vm: AppViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    var text by remember { mutableStateOf("") }
    var providerId by remember { mutableStateOf(state.providers.firstOrNull()?.id ?: "") }
    Column(Modifier.padding(16.dp)) {
        Text("Импорт ключей", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        if (state.providers.isNotEmpty()) {
            Text("Провайдер:")
            state.providers.forEach { p ->
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(selected = p.id == providerId, onClick = { providerId = p.id })
                    Text(p.name)
                }
            }
        }
        OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Текст с ключами") }, modifier = Modifier.fillMaxWidth().height(200.dp))
        Spacer(Modifier.height(8.dp))
        Row {
            Button(enabled = providerId.isNotBlank() && text.isNotBlank(), onClick = {
                vm.importKeys(providerId, text)
                onBack()
            }) { Text("Парсить") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onBack) { Text("Назад") }
        }
    }
}
