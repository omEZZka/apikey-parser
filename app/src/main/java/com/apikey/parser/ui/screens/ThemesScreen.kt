package com.apikey.parser.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apikey.parser.ui.theme.Themes
import com.apikey.parser.vm.AppViewModel

@Composable
fun ThemesScreen(vm: AppViewModel, onBack: () -> Unit) {
    val state by vm.state.collectAsState()
    Column(Modifier.padding(16.dp)) {
        Text("Темы", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.weight(1f)) {
            items(Themes) { t ->
                val idx = Themes.indexOf(t)
                Card(
                    Modifier.padding(4.dp).clickable { vm.setTheme(idx) },
                    colors = CardDefaults.cardColors(containerColor = t.scheme.primaryContainer)
                ) {
                    Column(Modifier.padding(8.dp)) {
                        Text(t.name, color = t.scheme.onPrimaryContainer)
                        if (state.themeIndex == idx) Text("✓", color = t.scheme.onPrimaryContainer)
                    }
                }
            }
        }
        OutlinedButton(onClick = onBack) { Text("Назад") }
    }
}
