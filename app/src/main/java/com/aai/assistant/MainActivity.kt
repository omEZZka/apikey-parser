package com.aai.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aai.assistant.service.OverlayService
import com.aai.assistant.ui.chat.ChatScreen
import com.aai.assistant.ui.chat.ChatViewModel
import com.aai.assistant.ui.chat.SessionDrawer
import com.aai.assistant.ui.settings.SettingsScreen
import com.aai.assistant.ui.settings.SettingsViewModel
import com.aai.assistant.ui.theme.AaiTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            AaiTheme {
                val chatVm: ChatViewModel = viewModel()
                val settingsVm: SettingsViewModel = viewModel()
                val chatState by chatVm.state.collectAsState()
                val sessions by chatVm.sessions.collectAsState()
                val settingsState by settingsVm.state.collectAsState()
                val drawerState = rememberDrawerState(androidx.compose.material3.DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val context = LocalContext.current
                var showSettings by remember { mutableStateOf(false) }

                androidx.compose.runtime.LaunchedEffect(Unit) {
                    if (Settings.canDrawOverlays(context)) {
                        ContextCompat.startForegroundService(
                            context,
                            Intent(context, OverlayService::class.java)
                        )
                    }
                }

                if (showSettings) {
                    SettingsScreen(
                        state = settingsState,
                        onBack = { showSettings = false },
                        onUpdate = settingsVm::update,
                        onSave = settingsVm::save,
                        onLoadModels = settingsVm::loadModels,
                        onPickModel = settingsVm::pickModel,
                        onPreset = settingsVm::applyPreset,
                        onTest = settingsVm::testConnection,
                        onResetPrompt = settingsVm::resetPrompt
                    )
                } else {
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            SessionDrawer(
                                sessions = sessions,
                                activeId = chatState.activeSessionId,
                                onSelect = { id ->
                                    chatVm.selectSession(id)
                                    scope.launch { drawerState.close() }
                                },
                                onNew = {
                                    chatVm.newSession()
                                    scope.launch { drawerState.close() }
                                },
                                onRename = chatVm::renameSession,
                                onDelete = chatVm::deleteSession
                            )
                        }
                    ) {
                        ChatScreen(
                            state = chatState,
                            sessionTitle = sessions.find { it.id == chatState.activeSessionId }?.title
                                ?: "aAi Agent",
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onOpenSettings = { showSettings = true },
                            onSend = { text, images -> chatVm.send(text, images) },
                            onStop = chatVm::stop,
                            onMinimize = {
                                if (Settings.canDrawOverlays(context)) {
                                    ContextCompat.startForegroundService(
                                        context,
                                        Intent(context, OverlayService::class.java)
                                            .setAction(OverlayService.ACTION_SHOW_PANEL)
                                    )
                                    moveTaskToBack(true)
                                } else {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            android.net.Uri.parse("package:${context.packageName}")
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}