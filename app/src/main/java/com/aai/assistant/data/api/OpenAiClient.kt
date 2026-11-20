package com.aai.assistant.data.api

import com.aai.assistant.data.prefs.AgentSettings
import com.aai.assistant.domain.AgentResult
import com.aai.assistant.domain.ChatMessage
import com.aai.assistant.domain.ChatRequest
import com.aai.assistant.domain.ModelInfo
import com.aai.assistant.domain.ModelsResponse
import com.aai.assistant.domain.StreamChunk
import com.aai.assistant.domain.ToolCall
import com.aai.assistant.domain.ToolSpec
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.get
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

class OpenAiClient {

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val client = HttpClient(OkHttp) {
        expectSuccess = false
        engine {
            config {
                connectTimeout(30, TimeUnit.SECONDS)
                readTimeout(300, TimeUnit.SECONDS)
                writeTimeout(60, TimeUnit.SECONDS)
                retryOnConnectionFailure(true)
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 600_000
            socketTimeoutMillis = 300_000
        }
    }

    suspend fun listModels(settings: AgentSettings): List<ModelInfo> {
        val response: HttpResponse = client.get("${settings.baseUrl}/models") {
            if (settings.apiKey.isNotBlank()) header("Authorization", "Bearer ${settings.apiKey}")
        }
        if (!response.status.isSuccess()) {
            throw ApiException("HTTP ${response.status.value}: ${response.bodyAsText().take(500)}")
        }
        return json.decodeFromString<ModelsResponse>(response.bodyAsText()).data
    }

    fun streamCompletion(
        settings: AgentSettings,
        messages: List<ChatMessage>,
        tools: List<ToolSpec>?
    ): Flow<StreamEvent> = flow {
        val request = ChatRequest(
            model = settings.model,
            messages = messages,
            stream = true,
            temperature = settings.temperature,
            maxTokens = settings.maxTokens,
            tools = tools?.ifEmpty { null },
            toolChoice = if (!tools.isNullOrEmpty()) "auto" else null
        )
        val body = json.encodeToString(ChatRequest.serializer(), request)
        client.preparePost("${settings.baseUrl}/chat/completions") {
            contentType(ContentType.Application.Json)
            if (settings.apiKey.isNotBlank()) header("Authorization", "Bearer ${settings.apiKey}")
            header("Accept", "text/event-stream")
            setBody(body)
        }.execute { response ->
            if (!response.status.isSuccess()) {
                throw ApiException("HTTP ${response.status.value}: ${response.bodyAsText().take(800)}")
            }
            val channel: ByteReadChannel = response.bodyAsChannel()
            val contentBuilder = StringBuilder()
            val toolAcc = mutableMapOf<Int, ToolCallBuilder>()
            var finishReason: String? = null
            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line() ?: break
                if (!line.startsWith("data:")) continue
                val data = line.removePrefix("data:").trim()
                if (data == "[DONE]") break
                if (data.isEmpty()) continue
                val chunk = runCatching { json.decodeFromString<StreamChunk>(data) }.getOrNull() ?: continue
                for (choice in chunk.choices) {
                    choice.delta.content?.let { piece ->
                        if (piece.isNotEmpty()) {
                            contentBuilder.append(piece)
                            emit(StreamEvent.TextDelta(piece))
                        }
                    }
                    choice.delta.toolCalls?.forEach { delta ->
                        val acc = toolAcc.getOrPut(delta.index) { ToolCallBuilder() }
                        delta.id?.let { acc.id = it }
                        delta.function?.name?.let { acc.name = it }
                        delta.function?.arguments?.let { acc.arguments.append(it) }
                    }
                    choice.finishReason?.let { finishReason = it }
                }
            }
            val calls = toolAcc.toSortedMap().values.mapNotNull { it.build() }
            emit(
                StreamEvent.Completed(
                    AgentResult(
                        content = contentBuilder.toString(),
                        toolCalls = calls,
                        finishReason = finishReason
                    )
                )
            )
        }
    }

    private class ToolCallBuilder {
        var id: String? = null
        var name: String? = null
        val arguments = StringBuilder()

        fun build(): ToolCall? {
            val n = name ?: return null
            return ToolCall(
                id = id ?: "call_${System.nanoTime()}",
                function = com.aai.assistant.domain.FunctionCall(n, arguments.toString().ifEmpty { "{}" })
            )
        }
    }
}

sealed class StreamEvent {
    data class TextDelta(val text: String) : StreamEvent()
    data class Completed(val result: AgentResult) : StreamEvent()
}

class ApiException(message: String) : Exception(message)