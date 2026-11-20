package com.aai.assistant.tools

import com.aai.assistant.domain.FunctionSpec
import com.aai.assistant.domain.ToolSpec
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

object ToolSchemas {

    fun stringParam(description: String): JsonObject = buildJsonObject {
        put("type", "string")
        put("description", description)
    }

    fun numberParam(description: String): JsonObject = buildJsonObject {
        put("type", "number")
        put("description", description)
    }

    fun intParam(description: String): JsonObject = buildJsonObject {
        put("type", "integer")
        put("description", description)
    }

    fun params(vararg props: Pair<String, JsonObject>, required: List<String>): JsonObject =
        buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                props.forEach { (name, schema) -> put(name, schema) }
            }
            putJsonArray("required") {
                required.forEach { add(it) }
            }
        }

    fun spec(name: String, description: String, parameters: JsonObject): ToolSpec =
        ToolSpec(function = FunctionSpec(name, description, parameters))
}