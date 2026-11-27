package com.apikey.parser.data.net

import com.apikey.parser.data.model.ProviderCatalog
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*
import java.util.concurrent.ConcurrentHashMap

class Api {
    private val client = HttpClient(OkHttp) {
        engine {
            config {
                dispatcher(okhttp3.Dispatcher().apply {
                    maxRequests = 64
                    maxRequestsPerHost = 16
                })
            }
        }
        install(ContentNegotiation) { json() }
        install(HttpTimeout) {
            connectTimeoutMillis = 5_000
            requestTimeoutMillis = 12_000
            socketTimeoutMillis = 12_000
        }
        expectSuccess = false
    }

    private data class CacheEntry(val valid: Boolean, val models: List<String>, val at: Long)
    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val cacheTtl = 10 * 60 * 1000L

    private fun normalize(base: String): String = base.trim().trimEnd('/')

    suspend fun fetchText(url: String): String? = try {
        client.get(url).body<String>()
    } catch (e: Exception) { null }

    suspend fun findKeyExample(providerBaseUrl: String): String? {
        val base = normalize(providerBaseUrl).removeSuffix("/v1").removeSuffix("/v1/")
        val profile = ProviderCatalog.detectByUrl(base)
        val candidates = buildList {
            profile?.docsUrl?.let { add(it) }
            add(base); add("$base/docs"); add("$base/openapi.json"); add("$base/swagger.json")
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
        val cacheKey = "$base|$key"
        val cached = cache[cacheKey]
        if (cached != null && System.currentTimeMillis() - cached.at < cacheTtl) {
            return cached.valid to cached.models
        }

        val profile = profileId?.let { id -> ProviderCatalog.profiles.find { it.id == id } }
            ?: ProviderCatalog.detectByUrl(base) ?: ProviderCatalog.detectByKey(key)

        val outcomes = if (profile != null) {
            val h = authHeader ?: profile.authHeader
            val p = authPrefix ?: profile.authPrefix
            val paths = listOfNotNull(modelsPath ?: profile.modelsPath, "/v1/models", "/models").distinct()
            paths.map { path -> "$base$path" to mapOf(h to (p + key)) }
        } else {
            listOf(
                "$base/v1/models" to mapOf("Authorization" to "Bearer $key"),
                "$base/models" to mapOf("Authorization" to "Bearer $key"),
                "$base/v1/models" to mapOf("x-api-key" to key)
            )
        }

        for ((url, headers) in outcomes) {
            try {
                val resp = client.get(url) { headers.forEach { (k, v) -> header(k, v) } }
                val code = resp.status.value
                if (code in 200..299) {
                    val models = extractModels(resp.body<JsonElement>())
                    cache[cacheKey] = CacheEntry(true, models, System.currentTimeMillis())
                    return true to models
                }
                if ((code == 401 || code == 403) && profile != null) {
                    cache[cacheKey] = CacheEntry(false, emptyList(), System.currentTimeMillis())
                    return false to emptyList()
                }
            } catch (e: Exception) { }
        }
        cache[cacheKey] = CacheEntry(false, emptyList(), System.currentTimeMillis())
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
        }.distinct()
        for (u in candidates) {
            try {
                val resp = client.get(u) { header(HttpHeaders.Authorization, "Bearer $key") }
                if (resp.status.value in 200..299) {
                    return summarizeBalance(resp.body<String>())
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
