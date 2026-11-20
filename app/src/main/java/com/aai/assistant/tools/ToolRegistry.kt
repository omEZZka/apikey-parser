package com.aai.assistant.tools

import android.content.Context
import com.aai.assistant.domain.AgentTool
import com.aai.assistant.domain.ToolSpec

class ToolRegistry(context: Context) {
    private val tools: Map<String, AgentTool> = listOf(
        GetScreenStateTool(),
        TapTool(),
        LongPressTool(),
        SwipeTool(),
        TypeTextTool(),
        GlobalActionTool(),
        OpenAppTool(context.applicationContext),
        HttpRequestTool(),
        WebSearchTool(),
        WebReadTool(),
        WaitTool(),
        ClipboardTool(context.applicationContext),
        RunShellTool(),
        FileReadTool(),
        FileWriteTool(),
        FileListTool(),
        FileDeleteTool()
    ).associateBy { it.name }

    val specs: List<ToolSpec> = tools.values.map { it.spec }

    fun get(name: String): AgentTool? = tools[name]
}