package com.aai.assistant.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aai.assistant.App
import com.aai.assistant.data.prefs.AgentSettings
import com.aai.assistant.domain.ModelInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AgentSettings = AgentSettings(),
    val models: List<ModelInfo> = emptyList(),
    val loadingModels: Boolean = false,
    val modelsError: String? = null,
    val saved: Boolean = false
)

class SettingsViewModel : ViewModel() {
    private val store = App.instance.settingsStore
    private val client = App.instance.client

    private val _state = MutableStateFlow(SettingsUiState(settings = store.current()))
    val state: StateFlow<SettingsUiState> = _state

    fun update(transform: (AgentSettings) -> AgentSettings) {
        _state.value = _state.value.copy(settings = transform(_state.value.settings), saved = false)
    }

    fun save() {
        store.update { _state.value.settings }
        _state.value = _state.value.copy(settings = store.current(), saved = true)
    }

    fun loadModels() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loadingModels = true, modelsError = null)
            try {
                val models = client.listModels(_state.value.settings)
                _state.value = _state.value.copy(models = models, loadingModels = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loadingModels = false,
                    modelsError = e.message ?: "Ошибка загрузки моделей"
                )
            }
        }
    }

    fun pickModel(id: String) {
        update { it.copy(model = id) }
    }
}