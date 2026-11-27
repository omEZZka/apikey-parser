package com.apikey.parser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apikey.parser.vm.AppViewModel

@Composable
fun AddProviderScreen(vm: AppViewModel, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var example by remember { mutableStateOf("") }
    var fetching by remember { mutableStateOf(false) }
    Column(Modifier.padding(16.dp)) {
        Text("Добавить провайдера", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Имя") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Base URL") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row {
            Button(onClick = {
                fetching = true
                // fire and forget; example will be fetched in vm.addProvider too
            }) { Text("Найти пример в доках") }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = example, onValueChange = { example = it }, label = { Text("Пример ключа (если не найден)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Row {
            Button(onClick = {
                vm.addProvider(name, url)
                if (example.isNotBlank()) {
                    val last = vm.state.value.providers.lastOrNull()
                    last?.let { vm.updateProvider(it.id) { p -> p.copy(keyExample = example, keyTemplate = example) } }
                }
                onBack()
            }) { Text("Сохранить") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onBack) { Text("Отмена") }
        }
    }
}
