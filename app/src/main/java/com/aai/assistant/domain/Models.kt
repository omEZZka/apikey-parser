package com.aai.assistant.domain

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Serializable(with = MessageContentSerializer::class)
sealed class MessageContent {
    data class Text(val text: String) : MessageContent()
    data class Parts(val parts: List<ContentPart>) : MessageContent()
}

@Serializable
data class ContentPart(
    val type: String,
    val text: String? = null,
    @SerialName("image_url") val imageUrl: ImageUrl? = null
)

@Serializable
data class ImageUrl(
    val url: String,
    val detail: String? = null
)

object MessageContentSerializer : KSerializer<MessageContent> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("MessageContent", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): MessageContent {
        val input = decoder as JsonDecoder
        val element = input.decodeJsonElement()
        return when (element) {
            is JsonPrimitive -> MessageContent.Text(element.content)
            is JsonArray -> MessageContent.Parts(
                element.map { input.json.decodeFromJsonElement(ContentPart.serializer(), it) }
            )
            else -> MessageContent.Text("")
        }
    }

    override fun serialize(encoder: Encoder, value: MessageContent) {
        val output = encoder as JsonEncoder
        when (value) {
            is MessageContent.Text -> output.encodeJsonElement(JsonPrimitive(value.text))
            is MessageContent.Parts -> output.encodeJsonElement(
                buildJsonArray {
                    value.parts.forEach { part ->
                        add(output.json.encodeToJsonElement(ContentPart.serializer(), part))
                    }
                }
            )
        }
    }
}

val StringListSerializer: KSerializer<List<String>> = kotlinx.serialization.builtins.ListSerializer(
    kotlinx.serialization.serializer<String>()
)

fun textContent(text: String?): MessageContent? = text?.let { MessageContent.Text(it) }

fun multimodalContent(text: String, imagesBase64: List<String>): MessageContent {
    if (imagesBase64.isEmpty()) return MessageContent.Text(text)
    val parts = mutableListOf<ContentPart>()
    if (text.isNotBlank()) parts.add(ContentPart(type = "text", text = text))
    imagesBase64.forEach { b64 ->
        parts.add(
            ContentPart(
                type = "image_url",
                imageUrl = ImageUrl(url = "data:image/jpeg;base64,$b64", detail = "auto")
            )
        )
    }
    return MessageContent.Parts(parts)
}

fun MessageContent.asText(): String = when (this) {
    is MessageContent.Text -> text
    is MessageContent.Parts -> parts.mapNotNull { it.text }.joinToString("\n")
}

@Serializable
data class ChatMessage(
    val role: String,
    val content: MessageContent? = null,
    @SerialName("tool_calls") val toolCalls: List<ToolCall>? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null,
    val name: String? = null
)

@Serializable
data class ToolCall(
    val id: String,
    val type: String = "function",
    val function: FunctionCall
)

@Serializable
data class FunctionCall(
    val name: String,
    val arguments: String
)

@Serializable
data class ToolSpec(
    val type: String = "function",
    val function: FunctionSpec
)

@Serializable
data class FunctionSpec(
    val name: String,
    val description: String,
    val parameters: JsonObject
)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = true,
    val temperature: Float = 0.7f,
    @SerialName("max_tokens") val maxTokens: Int = 4096,
    val tools: List<ToolSpec>? = null,
    @SerialName("tool_choice") val toolChoice: String? = null
)

@Serializable
data class StreamChunk(
    val choices: List<StreamChoice> = emptyList()
)

@Serializable
data class StreamChoice(
    val index: Int = 0,
    val delta: StreamDelta = StreamDelta(),
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class StreamDelta(
    val role: String? = null,
    val content: String? = null,
    @SerialName("tool_calls") val toolCalls: List<ToolCallDelta>? = null
)

@Serializable
data class ToolCallDelta(
    val index: Int = 0,
    val id: String? = null,
    val type: String? = null,
    val function: FunctionCallDelta? = null
)

@Serializable
data class FunctionCallDelta(
    val name: String? = null,
    val arguments: String? = null
)

@Serializable
data class ModelsResponse(
    val data: List<ModelInfo> = emptyList()
)

@Serializable
data class ModelInfo(
    val id: String
)

data class AgentResult(
    val content: String,
    val toolCalls: List<ToolCall>,
    val finishReason: String?
)