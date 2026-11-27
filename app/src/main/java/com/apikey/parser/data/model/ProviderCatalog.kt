package com.apikey.parser.data.model

data class ProviderProfile(
    val id: String,
    val name: String,
    val hosts: List<String>,
    val baseUrl: String,
    val docsUrl: String,
    val keyPrefixes: List<String>,
    val keyRegex: String,
    val authHeader: String,
    val authPrefix: String,
    val modelsPath: String,
    val balancePath: String?
)

object ProviderCatalog {
    val profiles: List<ProviderProfile> = listOf(
        ProviderProfile("openai", "OpenAI", listOf("api.openai.com", "openai.com"), "https://api.openai.com", "https://platform.openai.com/docs", listOf("sk-", "sk-proj-"), "sk-[A-Za-z0-9_-]{20,}", "Authorization", "Bearer ", "/v1/models", "/dashboard/billing/credit_grants"),
        ProviderProfile("openrouter", "OpenRouter", listOf("openrouter.ai"), "https://openrouter.ai/api", "https://openrouter.ai/docs", listOf("sk-or-"), "sk-or-[A-Za-z0-9_-]{20,}", "Authorization", "Bearer ", "/v1/models", "/v1/credits"),
        ProviderProfile("groq", "Groq", listOf("api.groq.com", "groq.com"), "https://api.groq.com/openai", "https://console.groq.com/docs", listOf("gsk_"), "gsk_[A-Za-z0-9_-]{20,}", "Authorization", "Bearer ", "/v1/models", null),
        ProviderProfile("deepseek", "DeepSeek", listOf("api.deepseek.com", "deepseek.com"), "https://api.deepseek.com", "https://platform.deepseek.com/api-docs", listOf("sk-"), "sk-[A-Za-z0-9_-]{20,}", "Authorization", "Bearer ", "/v1/models", "/user/balance"),
        ProviderProfile("anthropic", "Anthropic", listOf("api.anthropic.com", "anthropic.com"), "https://api.anthropic.com", "https://docs.anthropic.com", listOf("sk-ant-"), "sk-ant-[A-Za-z0-9_-]{20,}", "x-api-key", "", "/v1/models", null),
        ProviderProfile("google", "Google Gemini", listOf("generativelanguage.googleapis.com", "googleapis.com"), "https://generativelanguage.googleapis.com", "https://ai.google.dev", listOf("AIza"), "AIza[0-9A-Za-z_-]{20,}", "x-goog-api-key", "", "/v1beta/models", null),
        ProviderProfile("mistral", "Mistral", listOf("api.mistral.ai", "mistral.ai"), "https://api.mistral.ai", "https://docs.mistral.ai", listOf(), "[A-Za-z0-9]{32}", "Authorization", "Bearer ", "/v1/models", null),
        ProviderProfile("xai", "xAI", listOf("api.x.ai", "x.ai"), "https://api.x.ai", "https://docs.x.ai", listOf("xai-"), "xai-[A-Za-z0-9_-]{20,}", "Authorization", "Bearer ", "/v1/models", null),
        ProviderProfile("together", "Together", listOf("api.together.xyz", "together.xyz"), "https://api.together.xyz", "https://docs.together.ai", listOf(), "[A-Za-z0-9]{40,}", "Authorization", "Bearer ", "/v1/models", null),
        ProviderProfile("fireworks", "Fireworks", listOf("api.fireworks.ai", "fireworks.ai"), "https://api.fireworks.ai", "https://docs.fireworks.ai", listOf(), "[A-Za-z0-9]{20,}", "Authorization", "Bearer ", "/v1/models", null),
        ProviderProfile("perplexity", "Perplexity", listOf("api.perplexity.ai", "perplexity.ai"), "https://api.perplexity.ai", "https://docs.perplexity.ai", listOf("pplx-"), "pplx-[A-Za-z0-9]{20,}", "Authorization", "Bearer ", "/v1/models", null),
        ProviderProfile("cohere", "Cohere", listOf("api.cohere.ai", "cohere.ai"), "https://api.cohere.ai", "https://docs.cohere.com", listOf(), "[A-Za-z0-9]{40}", "Authorization", "Bearer ", "/v1/models", null),
        ProviderProfile("huggingface", "HuggingFace", listOf("api-inference.huggingface.co", "huggingface.co"), "https://api-inference.huggingface.co", "https://huggingface.co/docs/api-inference", listOf("hf_"), "hf_[A-Za-z0-9]{20,}", "Authorization", "Bearer ", "/v1/models", null),
        ProviderProfile("replicate", "Replicate", listOf("api.replicate.com", "replicate.com"), "https://api.replicate.com", "https://replicate.com/docs", listOf("r8_"), "r8_[A-Za-z0-9]{20,}", "Authorization", "Bearer ", "/v1/models", null)
    )

    val genericRegex = Regex("[A-Za-z0-9_\\-]{20,}")
    val knownKeyRegexes = listOf(
        Regex("sk-or-[A-Za-z0-9_-]{20,}"),
        Regex("sk-proj-[A-Za-z0-9_-]{20,}"),
        Regex("sk-ant-[A-Za-z0-9_-]{20,}"),
        Regex("sk-[A-Za-z0-9_-]{20,}"),
        Regex("gsk_[A-Za-z0-9_-]{20,}"),
        Regex("xai-[A-Za-z0-9_-]{20,}"),
        Regex("pplx-[A-Za-z0-9]{20,}"),
        Regex("AIza[0-9A-Za-z_-]{20,}"),
        Regex("hf_[A-Za-z0-9]{20,}"),
        Regex("r8_[A-Za-z0-9]{20,}"),
        Regex("ghp_[A-Za-z0-9]{20,}")
    )

    fun detectByUrl(url: String): ProviderProfile? {
        val u = url.lowercase()
        return profiles.firstOrNull { p -> p.hosts.any { u.contains(it) } }
    }

    fun detectByKey(key: String): ProviderProfile? {
        return profiles
            .mapNotNull { p ->
                val best = p.keyPrefixes.filter { key.startsWith(it) }.maxByOrNull { it.length }
                if (best != null) p to best.length else null
            }
            .maxByOrNull { it.second }
            ?.first
    }

    fun extractKeys(text: String): List<String> {
        val found = linkedSetOf<String>()
        knownKeyRegexes.forEach { r -> r.findAll(text).forEach { found.add(it.value) } }
        if (found.isEmpty()) {
            Regex("[A-Za-z0-9_\\-]{32,}").findAll(text).forEach { found.add(it.value) }
        }
        return found.toList()
    }

    fun detectByKeyLoose(key: String): ProviderProfile? {
        val k = key.trim()
        return profiles.firstOrNull { p -> p.keyPrefixes.any { k.startsWith(it) } }
    }
}
