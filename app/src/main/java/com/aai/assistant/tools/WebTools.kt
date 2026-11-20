package com.aai.assistant.tools

import com.aai.assistant.domain.AgentTool
import com.aai.assistant.domain.ToolResult
import com.aai.assistant.domain.ToolSpec
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object WebClient {
    val http: HttpClient = HttpClient(OkHttp) {
        expectSuccess = false
        followRedirects = true
        engine {
            config {
                connectTimeout(20, TimeUnit.SECONDS)
                readTimeout(45, TimeUnit.SECONDS)
                retryOnConnectionFailure(true)
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            socketTimeoutMillis = 45_000
        }
    }

    const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36"
}

fun htmlToText(html: String): String {
    var text = html
    text = text.replace(Regex("(?is)<(script|style|noscript|svg|head)[^>]*>.*?</\\1>"), " ")
    text = text.replace(Regex("(?i)<br\\s*/?>"), "\n")
    text = text.replace(Regex("(?i)</(p|div|li|h[1-6]|tr|section|article)>"), "\n")
    text = text.replace(Regex("(?i)<li[^>]*>"), "• ")
    text = text.replace(Regex("(?i)<h([1-6])[^>]*>")) { m ->
        "\n" + "#".repeat(m.groupValues[1].toInt()) + " "
    }
    text = text.replace(Regex("(?is)<a[^>]*href=\"([^\"]*)\"[^>]*>(.*?)</a>")) { m ->
        val label = m.groupValues[2].replace(Regex("<[^>]+>"), "").trim()
        val href = m.groupValues[1]
        if (label.isNotEmpty() && href.startsWith("http")) "$label ($href)" else label
    }
    text = text.replace(Regex("<[^>]+>"), " ")
    text = text.replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<")
        .replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")
        .replace("&mdash;", "—").replace("&ndash;", "–").replace("&laquo;", "«").replace("&raquo;", "»")
    text = text.replace(Regex("&#(\\d+);")) { m ->
        m.groupValues[1].toIntOrNull()?.let { code ->
            runCatching { String(Character.toChars(code)) }.getOrNull()
        } ?: m.value
    }
    text = text.replace(Regex("[ \\t]+"), " ")
    text = text.replace(Regex("\\n\\s*\\n+"), "\n\n")
    return text.trim()
}

fun extractTitle(html: String): String =
    Regex("(?is)<title[^>]*>(.*?)</title>").find(html)?.groupValues?.get(1)
        ?.let { htmlToText(it) }?.trim().orEmpty()

class HttpRequestTool : AgentTool {
    override val name = "http_request"
    override val spec: ToolSpec = ToolSchemas.spec(
        name,
        "HTTP-запрос к любому URL или API прямо из приложения: GET/POST/PUT/DELETE/PATCH/HEAD. " +
            "Возвращает status, headers и body (до 40000 символов). Для JSON API передавай body как JSON-строку.",
        ToolSchemas.params(
            "url" to ToolSchemas.stringParam("Полный URL (https://…)"),
            "method" to ToolSchemas.stringParam("GET (по умолчанию), POST, PUT, DELETE, PATCH, HEAD"),
            "headers" to buildJsonObject {
                put("type", "object")
                put("description", "Заголовки запроса, объект {\"Header\": \"value\"}")
            },
            "body" to ToolSchemas.stringParam("Тело запроса (для POST/PUT/PATCH)"),
            required = listOf("url")
        )
    )

    override suspend fun execute(arguments: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        val url = arguments["url"]?.jsonPrimitive?.contentOrNull
            ?: return@withContext ToolResult.error("Нет аргумента url")
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return@withContext ToolResult.error("URL должен начинаться с http:// или https://")
        }
        val method = arguments["method"]?.jsonPrimitive?.contentOrNull?.uppercase() ?: "GET"
        val body = arguments["body"]?.jsonPrimitive?.contentOrNull
        try {
            val response = WebClient.http.request(url) {
                this.method = HttpMethod.parse(method)
                header("User-Agent", WebClient.USER_AGENT)
                arguments["headers"]?.jsonObject?.forEach { (k, v) ->
                    v.jsonPrimitive.contentOrNull?.let { header(k, it) }
                }
                if (body != null) {
                    val hasContentType = arguments["headers"]?.jsonObject?.keys
                        ?.any { it.equals("Content-Type", true) } == true
                    if (!hasContentType) header("Content-Type", "application/json")
                    setBody(body)
                }
            }
            val text = response.bodyAsText()
            val result = buildJsonObject {
                put("status", response.status.value)
                put("url", response.call.request.url.toString())
                put("contentType", response.headers["Content-Type"] ?: "")
                put("body", if (text.length > 40_000) text.take(40_000) + "\n…[обрезано]" else text)
            }
            ToolResult(result.toString(), isError = response.status.value >= 400)
        } catch (e: Exception) {
            ToolResult.error("Сетевая ошибка: ${e.message}")
        }
    }
}

class WebSearchTool : AgentTool {
    override val name = "web_search"
    override val spec: ToolSpec = ToolSchemas.spec(
        name,
        "Поиск в интернете (DuckDuckGo, без API-ключа). Возвращает список результатов: заголовок, URL, сниппет. " +
            "Для деталей открой лучший результат через web_read.",
        ToolSchemas.params(
            "query" to ToolSchemas.stringParam("Поисковый запрос"),
            "max_results" to ToolSchemas.intParam("Сколько результатов вернуть (по умолчанию 6, максимум 12)"),
            required = listOf("query")
        )
    )

    override suspend fun execute(arguments: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        val query = arguments["query"]?.jsonPrimitive?.contentOrNull
            ?: return@withContext ToolResult.error("Нет аргумента query")
        val max = (arguments["max_results"]?.jsonPrimitive?.intOrNull ?: 6).coerceIn(1, 12)
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val html = WebClient.http.request("https://html.duckduckgo.com/html/?q=$encoded") {
                header("User-Agent", WebClient.USER_AGENT)
            }.bodyAsText()
            val results = mutableListOf<String>()
            val blockRegex = Regex("(?is)<a[^>]*class=\"result__a\"[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>")
            val snippetRegex = Regex("(?is)<a[^>]*class=\"result__snippet\"[^>]*>(.*?)</a>")
            val blocks = blockRegex.findAll(html).toList()
            val snippets = snippetRegex.findAll(html).toList()
            for ((i, m) in blocks.withIndex()) {
                if (results.size >= max) break
                var href = m.groupValues[1]
                if (href.contains("uddg=")) {
                    href = runCatching {
                        java.net.URLDecoder.decode(href.substringAfter("uddg=").substringBefore("&"), "UTF-8")
                    }.getOrDefault(href)
                }
                if (href.startsWith("//")) href = "https:$href"
                val title = htmlToText(m.groupValues[2])
                val snippet = snippets.getOrNull(i)?.let { htmlToText(it.groupValues[1]) }.orEmpty()
                results.add("${results.size + 1}. $title\n   $href\n   $snippet")
            }
            if (results.isEmpty()) {
                return@withContext ToolResult("Ничего не найдено по запросу «$query». Попробуй переформулировать.")
            }
            ToolResult("Результаты поиска «$query»:\n\n" + results.joinToString("\n\n"))
        } catch (e: Exception) {
            ToolResult.error("Ошибка поиска: ${e.message}")
        }
    }
}

class WebReadTool : AgentTool {
    override val name = "web_read"
    override val spec: ToolSpec = ToolSchemas.spec(
        name,
        "Скачивает веб-страницу и возвращает её как чистый читаемый текст (без HTML/скриптов), до 30000 символов. " +
            "Используй после web_search или для чтения статей/документации.",
        ToolSchemas.params(
            "url" to ToolSchemas.stringParam("URL страницы"),
            "max_chars" to ToolSchemas.intParam("Лимит символов (по умолчанию 15000, максимум 30000)"),
            required = listOf("url")
        )
    )

    override suspend fun execute(arguments: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        val url = arguments["url"]?.jsonPrimitive?.contentOrNull
            ?: return@withContext ToolResult.error("Нет аргумента url")
        val max = (arguments["max_chars"]?.jsonPrimitive?.intOrNull ?: 15_000).coerceIn(1_000, 30_000)
        try {
            val response = WebClient.http.request(url) {
                header("User-Agent", WebClient.USER_AGENT)
                header("Accept", "text/html,application/xhtml+xml,text/plain,application/json")
            }
            if (response.status.value >= 400) {
                return@withContext ToolResult.error("HTTP ${response.status.value} для $url")
            }
            val raw = response.bodyAsText()
            val contentType = response.headers["Content-Type"].orEmpty()
            val (title, text) = if (contentType.contains("html") || raw.trimStart().startsWith("<")) {
                extractTitle(raw) to htmlToText(raw)
            } else {
                "" to raw
            }
            val header = buildString {
                append("URL: ").append(response.call.request.url.toString()).append('\n')
                if (title.isNotEmpty()) append("Заголовок: ").append(title).append('\n')
                append('\n')
            }
            ToolResult(header + if (text.length > max) text.take(max) + "\n…[обрезано]" else text)
        } catch (e: Exception) {
            ToolResult.error("Ошибка загрузки страницы: ${e.message}")
        }
    }
}