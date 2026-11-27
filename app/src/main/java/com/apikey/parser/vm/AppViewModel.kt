package com.apikey.parser.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apikey.parser.data.model.ApiKey
import com.apikey.parser.data.model.Provider
import com.apikey.parser.data.model.ProviderCatalog
import com.apikey.parser.data.model.ProviderProfile
import com.apikey.parser.data.net.Api
import com.apikey.parser.data.smart.SmartParser
import com.apikey.parser.data.store.Store
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)
    private val api = Api()
    private val _state = MutableStateFlow(store.load())
    val state: StateFlow<com.apikey.parser.data.model.AppState> = _state.asStateFlow()

    fun persist() { store.save(_state.value) }

    private fun providerFromProfile(profile: ProviderProfile, name: String? = null): Provider = Provider(
        name = name ?: profile.name,
        baseUrl = profile.baseUrl,
        docsUrl = profile.docsUrl,
        modelsPath = profile.modelsPath,
        authHeader = profile.authHeader,
        authPrefix = profile.authPrefix,
        profileId = profile.id
    )

    fun addProvider(name: String, baseUrl: String) {
        val profile = ProviderCatalog.detectByUrl(baseUrl)
        val p = if (profile != null) {
            providerFromProfile(profile, name.ifBlank { profile.name }).copy(
                baseUrl = baseUrl.trim().trimEnd('/'),
                keyExample = profile.keyRegex
            )
        } else Provider(name = name, baseUrl = baseUrl.trim().trimEnd('/'))
        _state.value = _state.value.copy(providers = _state.value.providers + p)
        persist()
        viewModelScope.launch {
            val ex = api.findKeyExample(p.baseUrl)
            if (ex != null) updateProvider(p.id) { it.copy(keyExample = ex, keyTemplate = ex) }
        }
    }

    fun updateProvider(id: String, transform: (Provider) -> Provider) {
        _state.value = _state.value.copy(providers = _state.value.providers.map { if (it.id == id) transform(it) else it })
        persist()
    }

    fun removeProvider(id: String) {
        _state.value = _state.value.copy(
            providers = _state.value.providers.filter { it.id != id },
            keys = _state.value.keys.filter { it.providerId != id }
        )
        persist()
    }

    fun addKey(providerId: String, key: String, label: String = ""): Boolean {
        val trimmed = key.trim()
        if (trimmed.isBlank()) return false
        val existing = _state.value.keys.firstOrNull { it.providerId == providerId && it.key == trimmed }
        if (existing != null) {
            validateKey(existing.id)
            return false
        }
        val k = ApiKey(providerId = providerId, key = trimmed, label = label)
        _state.value = _state.value.copy(keys = _state.value.keys + k)
        persist()
        validateKey(k.id)
        return true
    }

    fun removeKey(id: String) {
        _state.value = _state.value.copy(keys = _state.value.keys.filter { it.id != id })
        persist()
    }

    fun toggleFavorite(id: String) {
        _state.value = _state.value.copy(keys = _state.value.keys.map { if (it.id == id) it.copy(favorite = !it.favorite) else it })
        persist()
    }

    private fun providerFor(key: ApiKey) = _state.value.providers.find { it.id == key.providerId }

    fun validateKey(id: String) {
        val k = _state.value.keys.find { it.id == id } ?: return
        val p = providerFor(k) ?: return
        updateKey(id) { it.copy(status = "checking") }
        viewModelScope.launch {
            val (ok, models) = api.validateKey(p.baseUrl, k.key, p.modelsPath, p.authHeader, p.authPrefix, p.profileId)
            val bal = if (ok) api.fetchBalance(p.baseUrl, k.key, p.balancePathOrNull(), p.profileId) else null
            updateKey(id) {
                it.copy(
                    status = if (ok) "valid" else "invalid",
                    models = models,
                    balance = bal ?: it.balance,
                    lastChecked = System.currentTimeMillis(),
                    expiresAt = if (ok) null else it.expiresAt
                )
            }
        }
    }

    private fun Provider.balancePathOrNull(): String? = profileId?.let { id -> ProviderCatalog.profiles.find { it.id == id }?.balancePath }

    private fun updateKey(id: String, transform: (ApiKey) -> ApiKey) {
        _state.value = _state.value.copy(keys = _state.value.keys.map { if (it.id == id) transform(it) else it })
        persist()
    }

    fun validateAll() {
        _state.value.keys.forEach { validateKey(it.id) }
    }

    fun setTheme(index: Int) {
        _state.value = _state.value.copy(themeIndex = index)
        persist()
    }

    fun setLanguage(lang: String) {
        _state.value = _state.value.copy(language = lang)
        persist()
    }

    fun setConcurrency(n: Int) {
        _state.value = _state.value.copy(concurrency = n.coerceIn(1, 32))
        persist()
    }

    fun importKeys(providerId: String, text: String) {
        val p = _state.value.providers.find { it.id == providerId } ?: return
        val candidates = smartKeys(p, text)
        candidates.forEach { addKey(providerId, it) }
    }

    private fun smartKeys(p: Provider, text: String): List<String> {
        val profile = p.profileId?.let { id -> ProviderCatalog.profiles.find { it.id == id } }
            ?: ProviderCatalog.detectByUrl(p.baseUrl)
        if (profile != null) {
            val re = Regex(profile.keyRegex)
            val found = re.findAll(text).map { it.value }.toList()
            if (found.isNotEmpty()) return found.distinct()
        }
        val template = p.keyExample ?: p.keyTemplate
        if (template != null && !template.startsWith("[")) {
            val loose = template.map { c -> if (c.isLetterOrDigit()) "\\w" else Regex.escape(c.toString()) }.joinToString("")
            return try { Regex(loose).findAll(text).map { it.value }.distinct().toList() } catch (e: Exception) { emptyList() }
        }
        return ProviderCatalog.extractKeys(text)
    }

    data class SmartImportReport(
        val providersCreated: Int,
        val keysAdded: Int,
        val keysDuplicate: Int,
        val providers: List<String>
    )

    fun smartImport(text: String): SmartImportReport {
        val parsed = SmartParser.parse(text)
        var created = 0
        val providerNames = mutableListOf<String>()
        val state = _state.value
        val providers = state.providers.toMutableList()

        // ensure providers for known profiles found in the text
        for (profile in parsed.profiles) {
            if (providers.none { it.profileId == profile.id || it.baseUrl.contains(profile.hosts.first()) }) {
                providers.add(providerFromProfile(profile))
                created++
                providerNames.add(profile.name)
            }
        }
        // ensure providers for discovered URLs
        for (url in parsed.urls) {
            val profile = ProviderCatalog.detectByUrl(url)
            if (profile != null) {
                if (providers.none { it.profileId == profile.id }) {
                    providers.add(providerFromProfile(profile))
                    created++
                    providerNames.add(profile.name)
                }
            } else if (providers.none { it.baseUrl.equals(url.trimEnd('/'), true) }) {
                val host = url.removePrefix("https://").removePrefix("http://").substringBefore('/')
                providers.add(Provider(name = host, baseUrl = url.trimEnd('/')))
                created++
                providerNames.add(host)
            }
        }
        _state.value = _state.value.copy(providers = providers)

        var added = 0
        var dup = 0
        for (key in parsed.keys) {
            val profile = ProviderCatalog.detectByKey(key)
            val provider = if (profile != null) providers.firstOrNull { it.profileId == profile.id }
                ?: providers.firstOrNull { it.baseUrl.contains(profile.hosts.first()) }
            else providers.firstOrNull()
            if (provider == null) continue
            if (addKey(provider.id, key, "smart")) added++ else dup++
        }
        persist()
        return SmartImportReport(created, added, dup, providerNames.distinct())
    }

    fun bruteForce(providerIds: List<String>, charset: String, length: Int, prefix: String, onFound: (String, String) -> Unit, onProgress: (Int, Int, String) -> Unit, onDone: () -> Unit) {
        val providers = _state.value.providers.filter { it.id in providerIds }
        if (providers.isEmpty() || charset.isEmpty() || length <= 0) { onDone(); return }
        val chars = charset.toList()
        val estimated = Math.pow(chars.size.toDouble(), length.toDouble())
        val cap = 200000
        val total = estimated.toInt().coerceAtMost(cap)
        viewModelScope.launch {
            for (p in providers) {
                val sem = Semaphore(_state.value.concurrency.coerceAtLeast(1))
                val indices = IntArray(length)
                var hasMore = true
                var count = 0
                val batch = mutableListOf<String>()
                val batchSize = (_state.value.concurrency.coerceAtLeast(1) * 4).coerceAtMost(64)
                while (hasMore && count < total) {
                    batch.clear()
                    repeat(batchSize) {
                        if (count + batch.size >= total) return@repeat
                        batch.add(prefix + indices.map { chars[it] }.joinToString(""))
                        var pos = length - 1
                        while (pos >= 0) {
                            indices[pos]++
                            if (indices[pos] < chars.size) break
                            indices[pos] = 0
                            pos--
                        }
                        if (pos < 0) hasMore = false
                    }
                    if (batch.isEmpty()) break
                    val currentBatch = batch.toList()
                    kotlinx.coroutines.coroutineScope {
                        val jobs = currentBatch.map { cand ->
                            async {
                                sem.acquire()
                                try {
                                    val (ok, _) = api.validateKey(p.baseUrl, cand, p.modelsPath, p.authHeader, p.authPrefix, p.profileId)
                                    if (ok) { addKey(p.id, cand, "bruteforce"); onFound(cand, p.name) }
                                } finally { sem.release() }
                            }
                        }
                        jobs.awaitAll()
                    }
                    count += currentBatch.size
                    onProgress(count, total, p.name)
                }
            }
            onDone()
        }
    }

    fun testCandidates(providerIds: List<String>, candidates: List<String>, onFound: (String, String) -> Unit, onProgress: (Int, Int, String) -> Unit, onDone: () -> Unit) {
        val providers = _state.value.providers.filter { it.id in providerIds }
        viewModelScope.launch {
            val total = candidates.size * providers.size
            var count = 0
            for (p in providers) {
                for (c in candidates) {
                    val (ok, _) = api.validateKey(p.baseUrl, c, p.modelsPath, p.authHeader, p.authPrefix, p.profileId)
                    count++
                    onProgress(count, total, p.name)
                    if (ok) { addKey(p.id, c, "import"); onFound(c, p.name) }
                }
            }
            onDone()
        }
    }

    fun detectPattern(providerId: String): String? {
        val keys = _state.value.keys.filter { it.providerId == providerId }.map { it.key }
        if (keys.size < 2) return null
        val len = keys.minOf { it.length }
        if (keys.any { it.length != len }) return null
        return (0 until len).map { pos ->
            val chars = keys.map { it[pos] }.toSet()
            if (chars.size == 1) keys[0][pos] else '?'
        }.joinToString("")
    }

    fun bruteForcePattern(providerIds: List<String>, onFound: (String, String) -> Unit, onProgress: (Int, Int, String) -> Unit, onDone: () -> Unit) {
        val providers = _state.value.providers.filter { it.id in providerIds }
        viewModelScope.launch {
            for (p in providers) {
                val keys = _state.value.keys.filter { it.providerId == p.id }.map { it.key }
                if (keys.size < 2) continue
                val len = keys.minOf { it.length }
                if (keys.any { it.length != len }) continue
                val varPositions = (0 until len).filter { pos -> keys.map { it[pos] }.toSet().size > 1 }
                val varCharsets = varPositions.map { pos -> keys.map { it[pos] }.toSet().joinToString("") }
                val total = varCharsets.fold(1) { acc, cs -> (acc * cs.length).coerceAtMost(200000) }
                val indices = IntArray(varPositions.size)
                var hasMore = true
                var count = 0
                val cap = 200000
                val sem = Semaphore(_state.value.concurrency.coerceAtLeast(1))
                while (hasMore && count < total && count < cap) {
                    val batch = mutableListOf<String>()
                    val batchSize = (_state.value.concurrency.coerceAtLeast(1) * 4).coerceAtMost(64)
                    repeat(batchSize) {
                        if (count + batch.size >= total || count + batch.size >= cap) return@repeat
                        val candChars = keys[0].toCharArray()
                        for (i in varPositions.indices) candChars[varPositions[i]] = varCharsets[i][indices[i]]
                        batch.add(String(candChars))
                        var pos = varPositions.size - 1
                        while (pos >= 0) {
                            indices[pos]++
                            if (indices[pos] < varCharsets[pos].length) break
                            indices[pos] = 0
                            pos--
                        }
                        if (pos < 0) hasMore = false
                    }
                    if (batch.isEmpty()) break
                    val currentBatch = batch.toList()
                    kotlinx.coroutines.coroutineScope {
                        val jobs = currentBatch.map { cand ->
                            async {
                                sem.acquire()
                                try {
                                    val (ok, _) = api.validateKey(p.baseUrl, cand, p.modelsPath, p.authHeader, p.authPrefix, p.profileId)
                                    if (ok) { addKey(p.id, cand, "pattern"); onFound(cand, p.name) }
                                } finally { sem.release() }
                            }
                        }
                        jobs.awaitAll()
                    }
                    count += currentBatch.size
                    onProgress(count, total, p.name)
                }
            }
            onDone()
        }
    }
}
