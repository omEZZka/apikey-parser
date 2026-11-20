package com.aai.assistant.service

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.aai.assistant.App
import com.aai.assistant.ui.chat.ChatUiState
import com.aai.assistant.ui.overlay.OverlayPanel
import com.aai.assistant.ui.theme.AaiTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class OverlayPanelHost(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    viewModelStoreOwner: ViewModelStoreOwner,
    savedStateOwner: SavedStateRegistryOwner,
    private val onClose: () -> Unit
) : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val repo = App.instance.repository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val stateFlow = kotlinx.coroutines.flow.MutableStateFlow(ChatUiState())

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    val view: ComposeView = ComposeView(context).apply {
        setViewTreeLifecycleOwner(this@OverlayPanelHost)
        setViewTreeViewModelStoreOwner(this@OverlayPanelHost)
        setViewTreeSavedStateRegistryOwner(this@OverlayPanelHost)
        setContent {
            AaiTheme {
                val state by stateFlow.collectAsState()
                OverlayPanel(
                    state = state,
                    onSend = { text -> send(text) },
                    onClose = onClose
                )
            }
        }
    }

    init {
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        scope.launch {
            val sessions = repo.observeSessions()
            sessions.collect { list ->
                val active = stateFlow.value.activeSessionId ?: list.firstOrNull()?.id
                if (active != null && stateFlow.value.activeSessionId == null) {
                    attach(active)
                }
            }
        }
        scope.launch {
            repo.events.collect { event ->
                when (event) {
                    is com.aai.assistant.data.repo.AgentEvent.Done ->
                        if (event.sessionId == stateFlow.value.activeSessionId) {
                            stateFlow.value = stateFlow.value.copy(isRunning = false)
                        }
                    is com.aai.assistant.data.repo.AgentEvent.Failed ->
                        if (event.sessionId == stateFlow.value.activeSessionId) {
                            stateFlow.value = stateFlow.value.copy(error = event.error)
                        }
                    else -> Unit
                }
            }
        }
    }

    private var messagesJob: kotlinx.coroutines.Job? = null

    private fun attach(sessionId: Long) {
        stateFlow.value = ChatUiState(activeSessionId = sessionId, isRunning = repo.isRunning(sessionId))
        messagesJob?.cancel()
        messagesJob = scope.launch {
            repo.observeMessages(sessionId).collect { messages ->
                stateFlow.value = stateFlow.value.copy(messages = messages)
            }
        }
    }

    private fun send(text: String) {
        scope.launch {
            var sessionId = stateFlow.value.activeSessionId
            if (sessionId == null) {
                sessionId = repo.createSession("Оверлей: " + text.take(30))
                attach(sessionId)
            }
            stateFlow.value = stateFlow.value.copy(isRunning = true, error = null)
            scope.launch(Dispatchers.IO) { repo.runAgentTurn(sessionId, text) }
        }
    }

    fun destroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        scope.cancel()
        store.clear()
    }
}