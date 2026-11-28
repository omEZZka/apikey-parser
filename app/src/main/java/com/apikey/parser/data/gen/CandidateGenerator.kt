package com.apikey.parser.data.gen

import com.apikey.parser.data.model.CharSetWeights
import com.apikey.parser.data.model.KeySegment
import com.apikey.parser.data.model.KeyStructure
import com.apikey.parser.data.model.charsetFamily
import java.util.HashSet
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.random.Random

private const val B64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

private val WORDS = listOf(
    "test", "demo", "temp", "dev", "prod", "live", "main", "beta", "alpha", "new",
    "key", "token", "secret", "api", "app", "user", "admin", "root", "guest", "service",
    "openai", "gpt", "chatgpt", "claude", "anthropic", "gemini", "groq", "deepseek", "mistral",
    "random", "sample", "example", "default", "master", "free", "premium", "pro", "trial",
    "project", "account", "client", "server", "mobile", "cloud", "studio", "lab", "team", "org",
    "summer", "winter", "spring", "autumn", "january", "monday", "friday", "hello", "world",
    "cat", "dog", "fox", "moon", "star", "sun", "sky", "sea", "fire", "ice", "night", "light",
    "abc", "xyz", "123", "2024", "2025", "2026", "0000", "1111", "password", "letmein"
)

class CandidateGenerator(
    initialKeys: List<String> = emptyList(),
    private val overrideCharset: String? = null
) {
    private val rng = Random(System.nanoTime() xor System.identityHashCode(this).toLong())
    private var structure: KeyStructure? = KeyStructure.analyze(initialKeys)
    private val examples = LinkedHashSet<String>()
    private val seen = HashSet<Long>()
    private val foundQueue = ConcurrentLinkedQueue<String>()
    private var generated = 0L
    private var rejected = 0L

    init {
        initialKeys.filter { it.isNotBlank() }.forEach {
            examples.add(it.trim())
            seen.add(hash(it.trim()))
        }
    }

    fun addFound(key: String) { foundQueue.add(key) }

    fun stats(): Pair<Long, Long> = generated to rejected

    fun next(): String {
        drain()
        val st = structure
        val ex = examples.toList()
        repeat(16) {
            val candidate = when (rng.nextInt(100)) {
                in 0..34 -> mutate(st, ex)
                in 35..59 -> randomByStructure(st)
                in 60..79 -> encoded(st)
                else -> mnemonic(st)
            }
            if (candidate.isNotBlank()) {
                val h = hash(candidate)
                if (seen.add(h)) {
                    generated++
                    return candidate
                }
            }
            rejected++
        }
        val fallback = randomByStructure(st) ?: rawRandom()
        seen.add(hash(fallback))
        generated++
        return fallback
    }

    private fun drain() {
        while (true) {
            val k = foundQueue.poll() ?: break
            if (examples.add(k)) {
                structure = KeyStructure.analyze(examples.toList()) ?: structure
            }
        }
    }

    private fun weightsFor(w: CharSetWeights?): CharSetWeights =
        if (overrideCharset.isNullOrBlank()) w ?: CharSetWeights.uniform("abcdefghijklmnopqrstuvwxyz0123456789")
        else CharSetWeights.uniform(overrideCharset)

    private fun randomByStructure(st: KeyStructure?): String? {
        if (st == null) return rawRandom()
        return buildString {
            for (seg in st.segments) {
                if (seg.fixed) append(seg.text)
                else repeat(seg.length) { i -> append(weightsFor(seg.weights.getOrNull(i)).pick(rng)) }
            }
        }
    }

    private fun rawRandom(): String = buildString {
        repeat(24) { append(B64[rng.nextInt(B64.length)]) }
    }

    private fun mutate(st: KeyStructure?, ex: List<String>): String? {
        if (ex.isEmpty()) return randomByStructure(st)
        val base = ex[rng.nextInt(ex.size)]
        val chars = base.toCharArray()
        val pos = st?.variablePositions()?.filter { it < chars.size }
        if (pos.isNullOrEmpty()) {
            repeat(1 + rng.nextInt(3)) {
                val p = rng.nextInt(chars.size)
                chars[p] = B64[rng.nextInt(B64.length)]
            }
            return String(chars)
        }
        repeat(1 + rng.nextInt(3)) {
            when (rng.nextInt(100)) {
                in 0..49 -> {
                    val p = pos[rng.nextInt(pos.size)]
                    chars[p] = weightsFor(st!!.weightAt(p)).pick(rng)
                }
                in 50..59 -> {
                    val a = pos[rng.nextInt(pos.size)]
                    val b = pos[rng.nextInt(pos.size)]
                    val t = chars[a]; chars[a] = chars[b]; chars[b] = t
                }
                in 60..74 -> {
                    if (ex.size > 1) {
                        val donor = ex[rng.nextInt(ex.size)]
                        val seg = st!!.segments.filter { !it.fixed }.random(rng)
                        var off = 0
                        for (s in st.segments) {
                            if (s === seg) break
                            off += if (s.fixed) s.text.length else s.length
                        }
                        for (k in 0 until seg.length) {
                            if (off + k < chars.size && off + k < donor.length) chars[off + k] = donor[off + k]
                        }
                    }
                }
                in 75..84 -> {
                    val p = pos[rng.nextInt(pos.size)]
                    chars[p] = B64[rng.nextInt(B64.length)]
                }
                else -> {}
            }
        }
        return String(chars)
    }

    private fun encoded(st: KeyStructure?): String? {
        val pos = st?.variablePositions()
        val size = pos?.size?.takeIf { it > 0 } ?: 16
        val family = st?.segments?.firstOrNull { !it.fixed }?.weights?.firstOrNull()?.alphabet()?.let { charsetFamily(it) }
            ?: "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val bytes = ByteArray((size * 3 / 4).coerceAtLeast(4) + 2).also { rng.nextBytes(it) }
        val encoded = if (family.all { it in "0123456789abcdef" }) bytes.joinToString("") { "%02x".format(it) } else b64url(bytes)
        if (encoded.length < size) return null
        val source = encoded.toCharArray()
        if (pos == null) return String(source.copyOfRange(0, size))
        val out = CharArray(st!!.length)
        var offset = 0
        for (seg in st.segments) {
            if (seg.fixed) {
                seg.text.copyInto(out, offset)
                offset += seg.text.length
            } else {
                repeat(seg.length) {
                    out[offset + it] = weightsFor(seg.weights.getOrNull(it)).let { w ->
                        if (rng.nextInt(100) < 70) source[(it + seg.length * 3) % source.size] else w.pick(rng)
                    }
                }
                offset += seg.length
            }
        }
        return String(out)
    }

    private fun mnemonic(st: KeyStructure?): String? {
        val pos = st?.variablePositions()
        val size = pos?.size?.takeIf { it > 0 } ?: (12 + rng.nextInt(16))
        val parts = mutableListOf<String>()
        repeat(1 + rng.nextInt(3)) { parts.add(WORDS[rng.nextInt(WORDS.size)]) }
        var text = parts.joinToString(if (rng.nextBoolean()) "" else if (rng.nextBoolean()) "-" else "_")
        if (rng.nextInt(100) < 60) {
            text = buildString {
                for (c in text) append(when (c) {
                    'a' -> if (rng.nextInt(4) == 0) "4" else c
                    'e' -> if (rng.nextInt(4) == 0) "3" else c
                    'i' -> if (rng.nextInt(4) == 0) "1" else c
                    'o' -> if (rng.nextInt(4) == 0) "0" else c
                    's' -> if (rng.nextInt(4) == 0) "5" else c
                    else -> c
                })
            }
        }
        if (text.length < size) {
            val w = CharSetWeights.uniform("abcdefghijklmnopqrstuvwxyz0123456789")
            text += buildString { repeat(size - text.length) { append(w.pick(rng)) } }
        }
        if (text.length > size) text = text.take(size)
        if (pos == null) return text
        val out = CharArray(st!!.length)
        var src = 0
        var offset = 0
        for (seg in st.segments) {
            if (seg.fixed) {
                seg.text.copyInto(out, offset)
                offset += seg.text.length
            } else {
                repeat(seg.length) {
                    out[offset + it] = if (src < text.length) text[src++] else weightsFor(seg.weights.getOrNull(it)).pick(rng)
                }
                offset += seg.length
            }
        }
        return String(out)
    }

    private fun b64url(bytes: ByteArray): String = buildString {
        var i = 0
        while (i + 2 < bytes.size) {
            val v = (bytes[i].toInt() and 0xff shl 16) or (bytes[i + 1].toInt() and 0xff shl 8) or (bytes[i + 2].toInt() and 0xff)
            append(B64[(v shr 18) and 63]); append(B64[(v shr 12) and 63])
            append(B64[(v shr 6) and 63]); append(B64[v and 63])
            i += 3
        }
    }

    private fun hash(s: String): Long {
        var h = 0xcbf29ce484222325L
        for (c in s) h = (h xor c.code.toLong()) * 0x100000001b3L
        return h
    }
}
