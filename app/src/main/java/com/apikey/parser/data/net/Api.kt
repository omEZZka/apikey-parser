package com.apikey.parser.data.net

import com.apikey.parser.data.model.ProviderCatalog
import com.apikey.parser.data.model.ProviderProfile
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

class Api {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json() }
        expectSuccess = false
    }

    private fun normalize(base: String): String = base.trim().trimEnd('/')

    suspend fun fetchText(url: String): String? = try {
        client.get(url).body<String>()
    } catch (e: Exception) { null }

    suspend fun findKeyExample(providerBaseUrl: String): String? {
        val base = normalize(providerBaseUrl).removeSuffix("/v1").removeSuffix("/v1/")
        val profile = ProviderCatalog.detectByUrl(base)
        val candidates = buildList {
            profile?.docsUrl?.let { add(it) }
            add(base); add("$base/"); add("$base/docs"); add("$base/api")
            add("$base/reference"); add("$base/v1"); add("$base/openapi.json")
            add("$base/swagger.json"); add("$base/redoc"); add("$base/api-docs")
        }.distinct()
        val generic = Regex("(?i)(api[_-]?key|token|secret|bearer)[^\\n]{0,60}?([A-Za-z0-9_\\-]{16,})")
        for (u in candidates) {
            val t = fetchText(u) ?: continue
            for (p in ProviderCatalog.knownKeyRegexes) {
                val m = p.find(t)
                if (m != null) return m.value
            }
            val gm = generic.find(t)
            if (gm != null) {
                val g = gm.groupValues.getOrElse(2) { gm.value }
                if (g.length >= 16) return g
            }
        }
        return null
    }

    suspend fun validateKey(
        baseUrl: String,
        key: String,
        modelsPath: String? = null,
        authHeader: String? = null,
        authPrefix: String? = null,
        profileId: String? = null
    ): Pair<Boolean, List<String>> {
        val base = normalize(baseUrl)
        val profile = profileId?.let { id -> ProviderCatalog.profiles.find { it.id == id } }
            ?: ProviderCatalog.detectByUrl(base) ?: ProviderCatalog.detectByKey(key)
        val paths = linkedSetOf<String>()
        modelsPath?.let { paths.add(it) }
        profile?.modelsPath?.let { paths.add(it) }
        paths.add("/v1/models"); paths.add("/models"); paths.add("/v1/model/list")
        val headers = buildList {
            if (authHeader != null) add(authHeader to ((authPrefix ?: "") + key))
            if (profile != null) add(profile.authHeader to (profile.authPrefix + key))
            add("Authorization" to "Bearer $key")
            add("x-api-key" to key)
            add("x-goog-api-key" to key)
        }
        for (path in paths) {
            for ((h, v) in headers) {
                try {
                    val resp = client.get("$base$path") { header(h, v) }
                    if (resp.status.value in 200..299) {
                        val body = resp.body<JsonElement>()
                        return true to extractModels(body)
                    }
                    if (resp.status.value == 401 || resp.status.value == 403) {
                        // try next header style before declaring invalid
                    }
                } catch (e: Exception) { }
            }
        }
        return false to emptyList()
    }

    private fun extractModels(el: JsonElement): List<String> {
        val out = mutableListOf<String>()
        fun walk(e: JsonElement) {
            when (e) {
                is JsonArray -> e.forEach { walk(it) }
                is JsonObject -> {
                    val id = e["id"]?.jsonPrimitive?.contentOrNull
                        ?: e["name"]?.jsonPrimitive?.contentOrNull
                    if (id != null && id.length < 120) out.add(id)
                    e["data"]?.let { walk(it) }
                    e["models"]?.let { walk(it) }
                }
                else -> {}
            }
        }
        walk(el)
        return out.distinct().take(300)
    }

    suspend fun fetchBalance(baseUrl: String, key: String, balancePath: String? = null, profileId: String? = null): String? {
        val base = normalize(baseUrl)
        val profile = profileId?.let { id -> ProviderCatalog.profiles.find { it.id == id } }
            ?: ProviderCatalog.detectByUrl(base) ?: ProviderCatalog.detectByKey(key)
        val candidates = buildList {
            balancePath?.let { add("$base$it") }
            profile?.balancePath?.let { add("$base$it") }
            add("$base/v1/usage")
            add("$base/dashboard/billing/credit_grants")
            add("$base/v1/credits")
            add("$base/user/balance")
            add("$base/v1/organization/usage")
        }.distinct()
        for (u in candidates) {
            try {
                val resp = client.get(u) { header(HttpHeaders.Authorization, "Bearer $key") }
                if (resp.status.value in 200..299) {
                    val body = resp.body<String>()
                    return summarizeBalance(body)
                }
            } catch (e: Exception) { }
        }
        return null
    }

    private fun summarizeBalance(body: String): String {
        val keys = listOf("balance", "credits", "credit", "total_credits", "remaining", "available")
        fun find(e: JsonElement): String? {
            return when (e) {
                is JsonObject -> {
                    for (k in keys) {
                        val v = e[k]?.jsonPrimitive?.contentOrNull
                        if (v != null) return v
                    }
                    e.values.firstNotNullOfOrNull { find(it) }
                }
                is JsonArray -> e.firstNotNullOfOrNull { find(it) }
                else -> null
            }
        }
        return try {
            val el = Json.parseToJsonElement(body)
            find(el) ?: body.take(160)
        } catch (e: Exception) { body.take(160) }
    }
}
