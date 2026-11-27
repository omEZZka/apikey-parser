package com.apikey.parser.data.net

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
    }

    private fun normalize(base: String): String = base.trim().trimEnd('/')

    suspend fun fetchText(url: String): String? = try {
        client.get(url).body<String>()
    } catch (e: Exception) { null }

    suspend fun findKeyExample(providerBaseUrl: String): String? {
        val base = normalize(providerBaseUrl).removeSuffix("/v1").removeSuffix("/v1/")
        val candidates = listOf(
            base, "$base/", "$base/docs", "$base/api", "$base/reference",
            "$base/v1", "$base/openapi.json", "$base/swagger.json",
            "$base/redoc", "$base/api-docs", "$base/api/keys"
        )
        val patterns = listOf(
            Regex("sk-[A-Za-z0-9_\-]{10,}"),
            Regex("gsk_[A-Za-z0-9_\-]{10,}"),
            Regex("ghp_[A-Za-z0-9]{10,}"),
            Regex("AIza[0-9A-Za-z_\-]{10,}"),
            Regex("xai-[A-Za-z0-9_\-]{10,}"),
            Regex("r8_[A-Za-z0-9]{10,}"),
            Regex("key-[A-Za-z0-9_\-]{10,}")
        )
        val generic = Regex("(?i)(api[_-]?key|token|secret|bearer)[^\n]{0,60}?([A-Za-z0-9_\-]{16,})")
        for (u in candidates) {
            val t = fetchText(u) ?: continue
            for (p in patterns) {
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

    suspend fun validateKey(baseUrl: String, key: String): Pair<Boolean, List<String>> {
        val base = normalize(baseUrl)
        val paths = listOf("/v1/models", "/models", "/v1/model/list")
        for (p in paths) {
            try {
                val resp = client.get("$base$p") {
                    header(HttpHeaders.Authorization, "Bearer $key")
                }
                if (resp.status.value in 200..299) {
                    val body = resp.body<JsonElement>()
                    val models = extractModels(body)
                    return true to models
                }
                if (resp.status.value == 401 || resp.status.value == 403) {
                    return false to emptyList()
                }
            } catch (e: Exception) { }
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
                    if (id != null) out.add(id)
                    e["data"]?.let { walk(it) }
                    e["models"]?.let { walk(it) }
                }
                else -> {}
            }
        }
        walk(el)
        return out.distinct()
    }

    suspend fun fetchBalance(baseUrl: String, key: String): String? {
        val base = normalize(baseUrl)
        val candidates = listOf(
            "$base/v1/usage",
            "$base/dashboard/billing/credit_grants",
            "$base/v1/organization/usage"
        )
        for (u in candidates) {
            try {
                val resp = client.get(u) { header(HttpHeaders.Authorization, "Bearer $key") }
                if (resp.status.value in 200..299) {
                    val body = resp.body<String>()
                    return body.take(200)
                }
            } catch (e: Exception) { }
        }
        return null
    }
}
