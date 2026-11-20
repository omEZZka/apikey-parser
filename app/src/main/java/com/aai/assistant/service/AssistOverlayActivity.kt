package com.aai.assistant.service

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aai.assistant.ui.chat.ChatViewModel
import com.aai.assistant.ui.overlay.MiniChatPanel
import com.aai.assistant.ui.theme.AaiTheme

class AssistOverlayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AaiTheme {
                val vm: ChatViewModel = viewModel()
                val state by vm.state.collectAsState()
                var compact by rememberSaveable { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { finish() }
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(horizontal = 12.dp, vertical = 20.dp)
                            .clickable(enabled = false) {}
                    ) {
                        MiniChatPanel(
                            state = state,
                            compact = compact,
                            onToggleCompact = { compact = !compact },
                            onSend = { text, images -> vm.send(text, images) },
                            onStop = vm::stop,
                            onClose = { finish() },
                            onDrag = { _, _ -> },
                            onOpenApp = {
                                startActivity(
                                    android.content.Intent(
                                        this@AssistOverlayActivity,
                                        com.aai.assistant.MainActivity::class.java
                                    ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }
}