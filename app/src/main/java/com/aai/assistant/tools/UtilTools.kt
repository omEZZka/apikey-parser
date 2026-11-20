package com.aai.assistant.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.aai.assistant.domain.AgentTool
import com.aai.assistant.domain.ToolResult
import com.aai.assistant.domain.ToolSpec
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class WaitTool : AgentTool {
    override val name = "wait"
    override val spec: ToolSpec = ToolSchemas.spec(
        name,
        "Пауза в миллисекундах (например, пока приложение загружается). Максимум 10000 мс.",
        ToolSchemas.params(
            "ms" to ToolSchemas.intParam("Длительность паузы в мс (по умолчанию 1000)"),
            required = emptyList()
        )
    )

    override suspend fun execute(arguments: JsonObject): ToolResult {
        val ms = (arguments["ms"]?.jsonPrimitive?.longOrNull ?: 1000L).coerceIn(100, 10_000)
        delay(ms)
        return ToolResult("Пауза ${ms}мс завершена")
    }
}

class ClipboardTool(private val context: Context) : AgentTool {
    override val name = "clipboard"
    override val spec: ToolSpec = ToolSchemas.spec(
        name,
        "Буфер обмена: action='read' — прочитать текущее содержимое, action='write' + text — записать текст.",
        ToolSchemas.params(
            "action" to ToolSchemas.stringParam("'read' или 'write'"),
            "text" to ToolSchemas.stringParam("Текст для записи (при action='write')"),
            required = listOf("action")
        )
    )

    override suspend fun execute(arguments: JsonObject): ToolResult {
        val action = arguments["action"]?.jsonPrimitive?.contentOrNull?.lowercase()
            ?: return ToolResult.error("Нет аргумента action")
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return when (action) {
            "read" -> {
                val text = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
                if (text.isNullOrEmpty()) ToolResult("(буфер обмена пуст)")
                else ToolResult(text.take(20_000))
            }
            "write" -> {
                val text = arguments["text"]?.jsonPrimitive?.contentOrNull
                    ?: return ToolResult.error("Нет аргумента text для записи")
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("aai", text))
                }
                ToolResult("Записано в буфер обмена: ${text.length} символов")
            }
            else -> ToolResult.error("Неизвестное действие '$action'. Используй 'read' или 'write'.")
        }
    }
}