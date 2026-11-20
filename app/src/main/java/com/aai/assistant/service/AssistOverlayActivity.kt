package com.aai.assistant.service

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.aai.assistant.ui.chat.ChatViewModel
import com.aai.assistant.ui.overlay.OverlayPanel
import com.aai.assistant.ui.theme.AaiTheme
import androidx.lifecycle.viewmodel.compose.viewModel

class AssistOverlayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AaiTheme {
                val vm: ChatViewModel = viewModel()
                val state by vm.state.collectAsState()
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
                            .clickable(enabled = false) {}
                    ) {
                        OverlayPanel(
                            state = state,
                            onSend = vm::send,
                            onClose = { finish() }
                        )
                    }
                }
            }
        }
    }
}