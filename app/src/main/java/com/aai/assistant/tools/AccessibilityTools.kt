package com.aai.assistant.tools

import com.aai.assistant.domain.AgentTool
import com.aai.assistant.domain.ToolResult
import com.aai.assistant.domain.ToolSpec
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.coroutines.resume

private fun notConnected(): ToolResult =
    ToolResult.error("AccessibilityService не подключён. Попроси пользователя включить 'aAi Device Control' в настройках специальных возможностей.")

class GetScreenStateTool : AgentTool {
    override val name = "get_screen_state"
    override val spec: ToolSpec = ToolSchemas.spec(
        name,
        "Возвращает JSON со всеми видимыми интерактивными элементами текущего экрана: id, text, desc, class, bounds (центр [x,y]), clickable/editable/scrollable. Всегда вызывай перед UI-действиями.",
        ToolSchemas.params(required = emptyList())
    )

    override suspend fun execute(arguments: JsonObject): ToolResult {
        val svc = ScreenBridge.service ?: return notConnected()
        val dump = svc.dumpScreen()
        return ToolResult(dump)
    }
}

class TapTool : AgentTool {
    override val name = "tap"
    override val spec: ToolSpec = ToolSchemas.spec(
        name,
        "Нажатие по координатам экрана (центр элемента из get_screen_state).",
        ToolSchemas.params(
            "x" to ToolSchemas.numberParam("X координата"),
            "y" to ToolSchemas.numberParam("Y координата"),
            required = listOf("x", "y")
        )
    )

    override suspend fun execute(arguments: JsonObject): ToolResult {
        val svc = ScreenBridge.service ?: return notConnected()
        val x = arguments["x"]?.jsonPrimitive?.floatOrNull ?: return ToolResult.error("Нет аргумента x")
        val y = arguments["y"]?.jsonPrimitive?.floatOrNull ?: return ToolResult.error("Нет аргумента y")
        val ok = suspendCancellableCoroutine { cont ->
            svc.tap(x, y) { result -> if (cont.isActive) cont.resume(result) }
        }
        return if (ok) ToolResult("Tap ($x, $y) выполнен") else ToolResult.error("Жест tap не выполнен")
    }
}

class LongPressTool : AgentTool {
    override val name = "long_press"
    override val spec: ToolSpec = ToolSchemas.spec(
        name,
        "Долгое нажатие по координатам.",
        ToolSchemas.params(
            "x" to ToolSchemas.numberParam("X координата"),
            "y" to ToolSchemas.numberParam("Y координата"),
            "duration_ms" to ToolSchemas.intParam("Длительность удержания в мс (по умолчанию 800)"),
            required = listOf("x", "y")
        )
    )

    override suspend fun execute(arguments: JsonObject): ToolResult {
        val svc = ScreenBridge.service ?: return notConnected()
        val x = arguments["x"]?.jsonPrimitive?.floatOrNull ?: return ToolResult.error("Нет аргумента x")
        val y = arguments["y"]?.jsonPrimitive?.floatOrNull ?: return ToolResult.error("Нет аргумента y")
        val duration = arguments["duration_ms"]?.jsonPrimitive?.longOrNull ?: 800L
        val ok = suspendCancellableCoroutine { cont ->
            svc.longPress(x, y, duration) { result -> if (cont.isActive) cont.resume(result) }
        }
        return if (ok) ToolResult("Long press ($x, $y, ${duration}ms) выполнен")
        else ToolResult.error("Жест long_press не выполнен")
    }
}

class SwipeTool : AgentTool {
    override val name = "swipe"
    override val spec: ToolSpec = ToolSchemas.spec(
        name,
        "Свайп/прокрутка от (x1,y1) к (x2,y2).",
        ToolSchemas.params(
            "x1" to ToolSchemas.numberParam("Начальная X"),
            "y1" to ToolSchemas.numberParam("Начальная Y"),
            "x2" to ToolSchemas.numberParam("Конечная X"),
            "y2" to ToolSchemas.numberParam("Конечная Y"),
            "duration_ms" to ToolSchemas.intParam("Длительность в мс (по умолчанию 300)"),
            required = listOf("x1", "y1", "x2", "y2")
        )
    )

    override suspend fun execute(arguments: JsonObject): ToolResult {
        val svc = ScreenBridge.service ?: return notConnected()
        val x1 = arguments["x1"]?.jsonPrimitive?.floatOrNull ?: return ToolResult.error("Нет x1")
        val y1 = arguments["y1"]?.jsonPrimitive?.floatOrNull ?: return ToolResult.error("Нет y1")
        val x2 = arguments["x2"]?.jsonPrimitive?.floatOrNull ?: return ToolResult.error("Нет x2")
        val y2 = arguments["y2"]?.jsonPrimitive?.floatOrNull ?: return ToolResult.error("Нет y2")
        val duration = arguments["duration_ms"]?.jsonPrimitive?.longOrNull ?: 300L
        val ok = suspendCancellableCoroutine { cont ->
            svc.swipe(x1, y1, x2, y2, duration) { result -> if (cont.isActive) cont.resume(result) }
        }
        return if (ok) ToolResult("Swipe ($x1,$y1)->($x2,$y2) выполнен")
        else ToolResult.error("Жест swipe не выполнен")
    }
}

class TypeTextTool : AgentTool {
    override val name = "type_text"
    override val spec: ToolSpec = ToolSchemas.spec(
        name,
        "Ввод текста в поле. Если указан node_id (из get_screen_state), вводит в это поле, иначе в сфокусированное. Фоллбэк: буфер обмена + вставка.",
        ToolSchemas.params(
            "text" to ToolSchemas.stringParam("Текст для ввода"),
            "node_id" to ToolSchemas.intParam("id поля из get_screen_state (необязательно)"),
            required = listOf("text")
        )
    )

    override suspend fun execute(arguments: JsonObject): ToolResult {
        val svc = ScreenBridge.service ?: return notConnected()
        val text = arguments["text"]?.jsonPrimitive?.contentOrNull ?: return ToolResult.error("Нет аргумента text")
        val nodeId = arguments["node_id"]?.jsonPrimitive?.intOrNull
        return ToolResult(svc.typeText(text, nodeId))
    }
}

class GlobalActionTool : AgentTool {
    override val name = "global_action"
    override val spec: ToolSpec = ToolSchemas.spec(
        name,
        "Системное действие: BACK, HOME, RECENTS, NOTIFICATIONS, QUICK_SETTINGS, POWER_DIALOG.",
        ToolSchemas.params(
            "action" to ToolSchemas.stringParam("Одно из: BACK, HOME, RECENTS, NOTIFICATIONS, QUICK_SETTINGS, POWER_DIALOG"),
            required = listOf("action")
        )
    )

    override suspend fun execute(arguments: JsonObject): ToolResult {
        val svc = ScreenBridge.service ?: return notConnected()
        val action = arguments["action"]?.jsonPrimitive?.contentOrNull?.uppercase()
            ?: return ToolResult.error("Нет аргумента action")
        return if (svc.globalAction(action)) ToolResult("Действие $action выполнено")
        else ToolResult.error("Действие $action не поддерживается или не выполнено")
    }
}