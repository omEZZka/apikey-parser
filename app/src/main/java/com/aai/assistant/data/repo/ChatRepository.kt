package com.aai.assistant.data.repo

import com.aai.assistant.data.api.OpenAiClient
import com.aai.assistant.data.api.StreamEvent
import com.aai.assistant.data.db.ChatDao
import com.aai.assistant.data.db.MessageEntity
import com.aai.assistant.data.db.SessionEntity
import com.aai.assistant.data.prefs.SettingsStore
import com.aai.assistant.domain.ChatMessage
import com.aai.assistant.domain.ToolCall
import com.aai.assistant.tools.ToolRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

sealed class AgentEvent {
    data class AssistantDelta(val sessionId: Long, val messageId: Long, val fullText: String) : AgentEvent()
    data class ToolStarted(val sessionId: Long, val call: ToolCall) : AgentEvent()
    data class ToolFinished(val sessionId: Long, val call: ToolCall, val output: String, val isError: Boolean) : AgentEvent()
    data class IterationInfo(val sessionId: Long, val iteration: Int, val max: Int) : AgentEvent()
    data class Failed(val sessionId: Long, val error: String) : AgentEvent()
    data class Done(val sessionId: Long) : AgentEvent()
}

class ChatRepository(
    private val dao: ChatDao,
    private val client: OpenAiClient,
    private val settingsStore: SettingsStore,
    private val toolRegistry: ToolRegistry
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }

    private val _events = MutableSharedFlow<AgentEvent>(extraBufferCapacity = 256)
    val events: SharedFlow<AgentEvent> = _events

    private val runningSessions = mutableSetOf<Long>()

    fun observeSessions(): Flow<List<SessionEntity>> = dao.observeSessions()
    fun observeMessages(sessionId: Long): Flow<List<MessageEntity>> = dao.observeMessages(sessionId)

    fun isRunning(sessionId: Long): Boolean = synchronized(runningSessions) { sessionId in runningSessions }

    suspend fun createSession(title: String = "Новый чат"): Long =
        dao.insertSession(SessionEntity(title = title))

    suspend fun renameSession(id: Long, title: String) = dao.renameSession(id, title)
    suspend fun deleteSession(id: Long) = dao.deleteSession(id)

    suspend fun runAgentTurn(sessionId: Long, userText: String) = withContext(Dispatchers.IO) {
        synchronized(runningSessions) {
            if (sessionId in runningSessions) return@withContext
            runningSessions.add(sessionId)
        }
        try {
            dao.insertMessage(MessageEntity(sessionId = sessionId, role = "user", content = userText))
            dao.touchSession(sessionId)
            maybeAutoTitle(sessionId, userText)
            agentLoop(sessionId)
        } catch (e: Exception) {
            val msg = e.message ?: e.javaClass.simpleName
            dao.insertMessage(MessageEntity(sessionId = sessionId, role = "error", content = msg, isError = true))
            _events.emit(AgentEvent.Failed(sessionId, msg))
        } finally {
            synchronized(runningSessions) { runningSessions.remove(sessionId) }
            _events.emit(AgentEvent.Done(sessionId))
        }
    }

    suspend fun continueAgent(sessionId: Long) = withContext(Dispatchers.IO) {
        synchronized(runningSessions) {
            if (sessionId in runningSessions) return@withContext
            runningSessions.add(sessionId)
        }
        try {
            agentLoop(sessionId)
        } catch (e: Exception) {
            val msg = e.message ?: e.javaClass.simpleName
            dao.insertMessage(MessageEntity(sessionId = sessionId, role = "error", content = msg, isError = true))
            _events.emit(AgentEvent.Failed(sessionId, msg))
        } finally {
            synchronized(runningSessions) { runningSessions.remove(sessionId) }
            _events.emit(AgentEvent.Done(sessionId))
        }
    }

    private suspend fun maybeAutoTitle(sessionId: Long, userText: String) {
        val session = dao.getSession(sessionId) ?: return
        if (session.title == "Новый чат") {
            val title = userText.replace('\n', ' ').trim().take(40).ifEmpty { "Новый чат" }
            dao.renameSession(sessionId, title)
        }
    }

    private suspend fun agentLoop(sessionId: Long) {
        val settings = settingsStore.current()
        if (settings.apiKey.isBlank() && !settings.baseUrl.contains("localhost") && !settings.baseUrl.contains("127.0.0.1")) {
            throw IllegalStateException("API Key не задан. Открой Настройки и укажи ключ и Base URL.")
        }
        var iteration = 0
        while (iteration < settings.maxIterations) {
            iteration++
            _events.emit(AgentEvent.IterationInfo(sessionId, iteration, settings.maxIterations))
            val history = buildHistory(sessionId, settings.systemPrompt)
            val assistantMsgId = dao.insertMessage(
                MessageEntity(sessionId = sessionId, role = "assistant", content = "")
            )
            var streamedText = ""
            var result: com.aai.assistant.domain.AgentResult? = null
            client.streamCompletion(settings, history, toolRegistry.specs).collect { event ->
                when (event) {
                    is StreamEvent.TextDelta -> {
                        streamedText += event.text
                        dao.updateMessageContent(assistantMsgId, streamedText)
                        _events.emit(AgentEvent.AssistantDelta(sessionId, assistantMsgId, streamedText))
                    }
                    is StreamEvent.Completed -> result = event.result
                }
            }
            val completed = result ?: throw IllegalStateException("Поток завершился без результата")
            if (completed.content != streamedText) {
                dao.updateMessageContent(assistantMsgId, completed.content)
            }
            if (completed.toolCalls.isEmpty()) {
                if (completed.content.isBlank()) {
                    dao.updateMessageContent(assistantMsgId, "(пустой ответ модели)")
                }
                dao.touchSession(sessionId)
                return
            }
            val callsJson = json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(ToolCall.serializer()),
                completed.toolCalls
            )
            dao.insertMessage(
                MessageEntity(
                    sessionId = sessionId,
                    role = "assistant_calls",
                    content = "",
                    toolCallsJson = callsJson
                )
            )
            for (call in completed.toolCalls) {
                _events.emit(AgentEvent.ToolStarted(sessionId, call))
                val toolResult = executeTool(call)
                dao.insertMessage(
                    MessageEntity(
                        sessionId = sessionId,
                        role = "tool",
                        content = toolResult.first,
                        toolCallId = call.id,
                        toolName = call.function.name,
                        isError = toolResult.second
                    )
                )
                _events.emit(AgentEvent.ToolFinished(sessionId, call, toolResult.first, toolResult.second))
            }
            dao.touchSession(sessionId)
        }
        dao.insertMessage(
            MessageEntity(
                sessionId = sessionId,
                role = "error",
                content = "Достигнут лимит итераций агента (${settings.maxIterations}). Отправь сообщение, чтобы продолжить.",
                isError = true
            )
        )
    }

    private suspend fun executeTool(call: ToolCall): Pair<String, Boolean> {
        val tool = toolRegistry.get(call.function.name)
            ?: return "Ошибка: неизвестный инструмент '${call.function.name}'" to true
        val args: JsonObject = try {
            json.parseToJsonElement(call.function.arguments.ifBlank { "{}" }) as JsonObject
        } catch (e: Exception) {
            return "Ошибка: невалидный JSON аргументов: ${e.message}" to true
        }
        return try {
            val result = tool.execute(args)
            result.output to result.isError
        } catch (e: Exception) {
            "Исключение в инструменте: ${e.message}" to true
        }
    }

    private suspend fun buildHistory(sessionId: Long, systemPrompt: String): List<ChatMessage> {
        val messages = mutableListOf(ChatMessage(role = "system", content = systemPrompt))
        for (entity in dao.getMessages(sessionId)) {
            when (entity.role) {
                "user" -> messages.add(ChatMessage(role = "user", content = entity.content))
                "assistant" -> if (entity.content.isNotBlank()) {
                    messages.add(ChatMessage(role = "assistant", content = entity.content))
                }
                "assistant_calls" -> {
                    val calls = entity.toolCallsJson?.let {
                        json.decodeFromString(
                            kotlinx.serialization.builtins.ListSerializer(ToolCall.serializer()), it
                        )
                    } ?: continue
                    messages.add(ChatMessage(role = "assistant", content = null, toolCalls = calls))
                }
                "tool" -> messages.add(
                    ChatMessage(
                        role = "tool",
                        content = entity.content,
                        toolCallId = entity.toolCallId,
                        name = entity.toolName
                    )
                )
            }
        }
        return messages
    }
}