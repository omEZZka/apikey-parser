package com.aai.assistant.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class AgentSettings(
    val baseUrl: String = "https://api.openai.com/v1",
    val apiKey: String = "",
    val model: String = "gpt-4o-mini",
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 4096,
    val maxIterations: Int = 25
) {
    companion object {
        const val DEFAULT_SYSTEM_PROMPT =
            "Ты — aAi, автономный AI-ассистент с полным доступом к Android-устройству пользователя. " +
                "У тебя есть инструменты: чтение экрана (get_screen_state), жесты (tap, long_press, swipe), " +
                "ввод текста (type_text), системные действия (global_action), открытие приложений (open_app), " +
                "shell-команды (run_shell) и работа с файлами (file_read, file_write, file_list, file_delete). " +
                "Работай по циклу: проанализируй задачу, вызови нужный инструмент, изучи результат, сделай следующий шаг, " +
                "пока задача не будет полностью выполнена. Перед действиями с UI всегда сначала вызывай get_screen_state, " +
                "чтобы видеть актуальные координаты элементов. Не выдумывай координаты. " +
                "Отвечай пользователю кратко и по делу, на языке пользователя."
    }
}

class SettingsStore(context: Context) {

    private val prefs: SharedPreferences = runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "aai_secure_settings",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as SharedPreferences
    }.getOrElse {
        context.getSharedPreferences("aai_settings_fallback", Context.MODE_PRIVATE)
    }

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AgentSettings> = _settings

    private fun load(): AgentSettings = AgentSettings(
        baseUrl = prefs.getString(KEY_BASE_URL, AgentSettings().baseUrl) ?: AgentSettings().baseUrl,
        apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
        model = prefs.getString(KEY_MODEL, AgentSettings().model) ?: AgentSettings().model,
        systemPrompt = prefs.getString(KEY_SYSTEM_PROMPT, AgentSettings.DEFAULT_SYSTEM_PROMPT)
            ?: AgentSettings.DEFAULT_SYSTEM_PROMPT,
        temperature = prefs.getFloat(KEY_TEMPERATURE, 0.7f),
        maxTokens = prefs.getInt(KEY_MAX_TOKENS, 4096),
        maxIterations = prefs.getInt(KEY_MAX_ITERATIONS, 25)
    )

    fun update(transform: (AgentSettings) -> AgentSettings) {
        val next = transform(_settings.value)
        prefs.edit()
            .putString(KEY_BASE_URL, next.baseUrl.trimEnd('/'))
            .putString(KEY_API_KEY, next.apiKey)
            .putString(KEY_MODEL, next.model)
            .putString(KEY_SYSTEM_PROMPT, next.systemPrompt)
            .putFloat(KEY_TEMPERATURE, next.temperature)
            .putInt(KEY_MAX_TOKENS, next.maxTokens)
            .putInt(KEY_MAX_ITERATIONS, next.maxIterations)
            .apply()
        _settings.value = next.copy(baseUrl = next.baseUrl.trimEnd('/'))
    }

    fun current(): AgentSettings = _settings.value

    companion object {
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL = "model"
        private const val KEY_SYSTEM_PROMPT = "system_prompt"
        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_MAX_TOKENS = "max_tokens"
        private const val KEY_MAX_ITERATIONS = "max_iterations"
    }
}