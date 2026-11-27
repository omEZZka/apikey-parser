package com.apikey.parser.data.smart

import com.apikey.parser.data.model.ProviderCatalog
import com.apikey.parser.data.model.ProviderProfile
import kotlinx.serialization.json.*

data class SmartParseResult(
    val urls: List<String>,
    val keys: List<String>,
    val profiles: List<ProviderProfile>
)

object SmartParser {
    private val urlRegex = Regex("https?://[^\\s\"'<>),\\]]+")

    fun parse(text: String): SmartParseResult {
        val urls = linkedSetOf<String>()
        val profiles = linkedSetOf<ProviderProfile>()
        val keys = linkedSetOf<String>()

        fun walkJson(el: JsonElement) {
            when (el) {
                is JsonPrimitive -> {
                    if (el.isString) {
                        val s = el.content
                        urlRegex.findAll(s).forEach { urls.add(it.value); ProviderCatalog.detectByUrl(it.value)?.let { p -> profiles.add(p) } }
                        ProviderCatalog.extractKeys(s).forEach { keys.add(it) }
                    }
                }
                is JsonObject -> el.values.forEach { walkJson(it) }
                is JsonArray -> el.forEach { walkJson(it) }
            }
        }

        try {
            val el = Json.parseToJsonElement(text)
            walkJson(el)
        } catch (e: Exception) {
            urlRegex.findAll(text).forEach { m ->
                urls.add(m.value)
                ProviderCatalog.detectByUrl(m.value)?.let { profiles.add(it) }
            }
            ProviderCatalog.extractKeys(text).forEach { keys.add(it) }
        }

        // detect provider from key prefix as primary signal
        keys.forEach { k -> ProviderCatalog.detectByKey(k)?.let { profiles.add(it) } }

        return SmartParseResult(urls.toList(), keys.toList(), profiles.toList())
    }

    fun keyProfile(key: String): ProviderProfile? = ProviderCatalog.detectByKey(key)
}
