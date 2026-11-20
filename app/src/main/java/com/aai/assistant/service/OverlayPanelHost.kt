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
import com.aai.assistant.ui.overlay.MiniChatPanel
import com.aai.assistant.ui.theme.AaiTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class OverlayPanelHost(
    context: Context,
    private val compactFlow: MutableStateFlow<Boolean>,
    private val onToggleCompact: () -> Unit,
    private val onClose: () -> Unit,
    private val onDrag: (Float, Float) -> Unit,
    private val onOpenApp: () -> Unit
) : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val repo = App.instance.repository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val stateFlow = MutableStateFlow(ChatUiState())

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    val view: ComposeView = ComposeView(context)

    private fun initContent() {
        view.apply {
            setViewTreeLifecycleOwner(this@OverlayPanelHost)
            setViewTreeViewModelStoreOwner(this@OverlayPanelHost)
            setViewTreeSavedStateRegistryOwner(this@OverlayPanelHost)
            setContent {
                AaiTheme {
                    val state by stateFlow.collectAsState()
                    val compact by compactFlow.collectAsState()
                    MiniChatPanel(
                        state = state,
                        compact = compact,
                        onToggleCompact = onToggleCompact,
                        onSend = { text, images -> send(text, images) },
                        onStop = { stateFlow.value.activeSessionId?.let { repo.stopAgent(it) } },
                        onClose = onClose,
                        onDrag = onDrag,
                        onOpenApp = onOpenApp
                    )
                }
            }
        }
    }

    init {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        initContent()
        scope.launch {
            repo.observeSessions().collect { list ->
                if (stateFlow.value.activeSessionId == null) {
                    list.firstOrNull()?.let { attach(it.id) }
                }
            }
        }
        scope.launch {
            repo.events.collect { event ->
                when (event) {
                    is com.aai.assistant.data.repo.AgentEvent.IterationInfo ->
                        if (event.sessionId == stateFlow.value.activeSessionId) {
                            stateFlow.value = stateFlow.value.copy(
                                iterationInfo = "шаг ${event.iteration}/${event.max}"
                            )
                        }
                    is com.aai.assistant.data.repo.AgentEvent.Done ->
                        if (event.sessionId == stateFlow.value.activeSessionId) {
                            stateFlow.value = stateFlow.value.copy(isRunning = false, iterationInfo = null)
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

    fun attach(sessionId: Long) {
        stateFlow.value = ChatUiState(activeSessionId = sessionId, isRunning = repo.isRunning(sessionId))
        messagesJob?.cancel()
        messagesJob = scope.launch {
            repo.observeMessages(sessionId).collect { messages ->
                stateFlow.value = stateFlow.value.copy(messages = messages)
            }
        }
    }

    fun isRunning(): Boolean = stateFlow.value.isRunning

    private fun send(text: String, images: List<String>) {
        scope.launch {
            var sessionId = stateFlow.value.activeSessionId
            if (sessionId == null) {
                sessionId = repo.createSession("Мини: " + text.ifBlank { "📷 Фото" }.take(30))
                attach(sessionId)
            }
            stateFlow.value = stateFlow.value.copy(isRunning = true, error = null)
            repo.launchAgentTurn(sessionId, text, images)
        }
    }

    fun destroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        scope.cancel()
        store.clear()
    }
}