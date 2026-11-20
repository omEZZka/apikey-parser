package com.aai.assistant.tools

import android.content.Context
import android.content.Intent
import com.aai.assistant.domain.AgentTool
import com.aai.assistant.domain.ToolResult
import com.aai.assistant.domain.ToolSpec
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class OpenAppTool(private val context: Context) : AgentTool {
    override val name = "open_app"
    override val spec: ToolSpec = ToolSchemas.spec(
        name,
        "Открывает приложение по имени пакета (com.android.settings) или по названию (Настройки, Telegram). Используй перед UI-действиями.",
        ToolSchemas.params(
            "package_or_name" to ToolSchemas.stringParam("Имя пакета или название приложения"),
            required = listOf("package_or_name")
        )
    )

    override suspend fun execute(arguments: JsonObject): ToolResult {
        val query = arguments["package_or_name"]?.jsonPrimitive?.contentOrNull
            ?: return ToolResult.error("Нет аргумента package_or_name")
        val pm = context.packageManager
        val direct = pm.getLaunchIntentForPackage(query)
        if (direct != null) {
            direct.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(direct)
            return ToolResult("Открыт пакет $query")
        }
        val apps = pm.getInstalledApplications(0)
        val match = apps.firstOrNull { app ->
            val label = pm.getApplicationLabel(app).toString()
            label.equals(query, ignoreCase = true) || label.contains(query, ignoreCase = true)
        } ?: apps.firstOrNull { app ->
            app.packageName.contains(query, ignoreCase = true) &&
                pm.getLaunchIntentForPackage(app.packageName) != null
        }
        if (match == null) return ToolResult.error("Приложение '$query' не найдено")
        val intent = pm.getLaunchIntentForPackage(match.packageName)
            ?: return ToolResult.error("У ${match.packageName} нет launcher activity")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return ToolResult("Открыто: ${pm.getApplicationLabel(match)} (${match.packageName})")
    }
}