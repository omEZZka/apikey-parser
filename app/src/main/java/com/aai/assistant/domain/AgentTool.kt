package com.aai.assistant.domain

interface AgentTool {
    val name: String
    val spec: ToolSpec
    suspend fun execute(arguments: kotlinx.serialization.json.JsonObject): ToolResult
}

data class ToolResult(
    val output: String,
    val isError: Boolean = false
) {
    companion object {
        fun error(message: String) = ToolResult(message, isError = true)
    }
}