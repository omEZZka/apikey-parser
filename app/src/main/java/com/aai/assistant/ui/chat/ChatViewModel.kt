package com.aai.assistant.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aai.assistant.App
import com.aai.assistant.data.db.MessageEntity
import com.aai.assistant.data.db.SessionEntity
import com.aai.assistant.data.repo.AgentEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ChatUiState(
    val activeSessionId: Long? = null,
    val messages: List<MessageEntity> = emptyList(),
    val isRunning: Boolean = false,
    val iterationInfo: String? = null,
    val error: String? = null
)

class ChatViewModel : ViewModel() {

    private val repo = App.instance.repository

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state

    val sessions: StateFlow<List<SessionEntity>> = repo.observeSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var messagesJob: Job? = null

    init {
        viewModelScope.launch {
            repo.events.collect { event ->
                when (event) {
                    is AgentEvent.IterationInfo ->
                        if (event.sessionId == _state.value.activeSessionId) {
                            _state.value = _state.value.copy(
                                iterationInfo = "Агент: шаг ${event.iteration}/${event.max}"
                            )
                        }
                    is AgentEvent.Failed ->
                        if (event.sessionId == _state.value.activeSessionId) {
                            _state.value = _state.value.copy(error = event.error)
                        }
                    is AgentEvent.Done ->
                        if (event.sessionId == _state.value.activeSessionId) {
                            _state.value = _state.value.copy(isRunning = false, iterationInfo = null)
                        }
                    else -> Unit
                }
            }
        }
        viewModelScope.launch {
            sessions.collect { list ->
                if (_state.value.activeSessionId == null && list.isNotEmpty()) {
                    selectSession(list.first().id)
                }
            }
        }
    }

    fun selectSession(id: Long) {
        if (_state.value.activeSessionId == id) return
        _state.value = ChatUiState(activeSessionId = id, isRunning = repo.isRunning(id))
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            repo.observeMessages(id).collect { messages ->
                _state.value = _state.value.copy(messages = messages)
            }
        }
    }

    fun newSession() {
        viewModelScope.launch {
            val id = repo.createSession()
            selectSession(id)
        }
    }

    fun renameSession(id: Long, title: String) {
        viewModelScope.launch { repo.renameSession(id, title) }
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch {
            repo.deleteSession(id)
            if (_state.value.activeSessionId == id) {
                _state.value = ChatUiState()
                val remaining = sessions.value.filter { it.id != id }
                if (remaining.isNotEmpty()) selectSession(remaining.first().id)
            }
        }
    }

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            var sessionId = _state.value.activeSessionId
            if (sessionId == null) {
                sessionId = repo.createSession()
                selectSession(sessionId)
            }
            _state.value = _state.value.copy(isRunning = true, error = null)
            repo.runAgentTurn(sessionId, trimmed)
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}