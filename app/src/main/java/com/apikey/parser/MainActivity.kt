package com.apikey.parser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.apikey.parser.ui.screens.AddProviderScreen
import com.apikey.parser.ui.screens.BruteForceScreen
import com.apikey.parser.ui.screens.HomeScreen
import com.apikey.parser.ui.screens.ImportScreen
import com.apikey.parser.ui.screens.KeyDetailScreen
import com.apikey.parser.ui.screens.SettingsScreen
import com.apikey.parser.ui.screens.ThemesScreen
import com.apikey.parser.ui.theme.ApiKeyParserTheme
import com.apikey.parser.vm.AppViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: AppViewModel = viewModel()
            val state by vm.state.collectAsState()
            ApiKeyParserTheme(state.themeIndex) {
                val nav = rememberNavController()
                NavHost(navController = nav, startDestination = "home") {
                    composable("home") {
                        HomeScreen(vm,
                            onAddProvider = { nav.navigate("add") },
                            onKey = { nav.navigate("key/$it") },
                            onImport = { nav.navigate("import") },
                            onBrute = { nav.navigate("brute") },
                            onSettings = { nav.navigate("settings") },
                            onThemes = { nav.navigate("themes") }
                        )
                    }
                    composable("add") { AddProviderScreen(vm) { nav.popBackStack() } }
                    composable("import") { ImportScreen(vm) { nav.popBackStack() } }
                    composable("key/{id}") { back ->
                        val id = back.arguments?.getString("id") ?: ""
                        KeyDetailScreen(vm, id) { nav.popBackStack() }
                    }
                    composable("brute") { BruteForceScreen(vm) { nav.popBackStack() } }
                    composable("settings") { SettingsScreen(vm, { nav.popBackStack() }, { nav.navigate("themes") }) }
                    composable("themes") { ThemesScreen(vm) { nav.popBackStack() } }
                }
            }
        }
    }
}
