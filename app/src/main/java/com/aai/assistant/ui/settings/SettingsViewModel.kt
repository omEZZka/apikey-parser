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
    val testing: Boolean = false,
    val testResult: String? = null,
    val testOk: Boolean = false,
    val saved: Boolean = false
)

data class ProviderPreset(val name: String, val baseUrl: String, val model: String)

val providerPresets = listOf(
    ProviderPreset("OpenAI", "https://api.openai.com/v1", "gpt-4o-mini"),
    ProviderPreset("OpenRouter", "https://openrouter.ai/api/v1", "anthropic/claude-sonnet-4"),
    ProviderPreset("DeepSeek", "https://api.deepseek.com/v1", "deepseek-chat"),
    ProviderPreset("Groq", "https://api.groq.com/openai/v1", "llama-3.3-70b-versatile"),
    ProviderPreset("Gemini", "https://generativelanguage.googleapis.com/v1beta/openai", "gemini-2.0-flash"),
    ProviderPreset("Mistral", "https://api.mistral.ai/v1", "mistral-large-latest"),
    ProviderPreset("Локальный", "http://127.0.0.1:8080/v1", "local-model")
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

    fun applyPreset(preset: ProviderPreset) {
        update { it.copy(baseUrl = preset.baseUrl, model = preset.model) }
    }

    fun resetPrompt() {
        update { it.copy(systemPrompt = AgentSettings.DEFAULT_SYSTEM_PROMPT) }
    }

    fun testConnection() {
        viewModelScope.launch {
            _state.value = _state.value.copy(testing = true, testResult = null)
            try {
                val start = System.currentTimeMillis()
                val models = client.listModels(_state.value.settings)
                val ms = System.currentTimeMillis() - start
                _state.value = _state.value.copy(
                    testing = false,
                    testOk = true,
                    models = models,
                    testResult = "Соединение OK: ${models.size} моделей, ${ms}мс"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    testing = false,
                    testOk = false,
                    testResult = e.message ?: "Ошибка соединения"
                )
            }
        }
    }
}