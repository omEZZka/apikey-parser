package com.apikey.parser.data.model

import kotlin.random.Random

private const val HEX = "0123456789abcdef"
private const val BASE36 = "0123456789abcdefghijklmnopqrstuvwxyz"
private const val BASE62 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
private const val BASE64URL = "$BASE62-_"

fun charsetFamily(chars: String): String {
    val set = chars.toSet()
    return when {
        set.all { it in HEX } -> HEX
        set.all { it in BASE36 } -> BASE36
        set.all { it in BASE64URL } -> BASE62
        else -> BASE62
    }
}

data class CharSetWeights(val chars: CharArray, val weights: FloatArray) {
    private val total: Float = weights.sum()

    fun pick(rng: Random): Char {
        var r = rng.nextFloat() * total
        for (i in chars.indices) {
            r -= weights[i]
            if (r <= 0f) return chars[i]
        }
        return chars.last()
    }

    fun alphabet(): String = chars.concatToString()

    companion object {
        fun uniform(chars: String): CharSetWeights {
            val c = chars.toSet().toCharArray()
            return CharSetWeights(c, FloatArray(c.size) { 1f })
        }

        fun learned(observed: String, family: String): CharSetWeights {
            val counts = LinkedHashMap<Char, Float>()
            observed.forEach { counts[it] = (counts[it] ?: 0f) + 2f }
            family.distinct().forEach { counts[it] = (counts[it] ?: 0f) + 1f }
            val c = counts.keys.toCharArray()
            return CharSetWeights(c, FloatArray(c.size) { i -> counts[c[i]] ?: 1f })
        }
    }
}

data class KeySegment(
    val fixed: Boolean,
    val text: String = "",
    val length: Int = 0,
    val weights: List<CharSetWeights> = emptyList()
)

data class KeyStructure(
    val segments: List<KeySegment>,
    val examples: List<String>,
    val template: String
) {
    val length: Int get() = segments.sumOf { if (it.fixed) it.text.length else it.length }

    fun variablePositions(): List<Int> {
        val out = mutableListOf<Int>()
        var pos = 0
        for (seg in segments) {
            if (!seg.fixed) repeat(seg.length) { out.add(pos + it) }
            pos += if (seg.fixed) seg.text.length else seg.length
        }
        return out
    }

    fun weightAt(position: Int): CharSetWeights? {
        var pos = 0
        for (seg in segments) {
            val size = if (seg.fixed) seg.text.length else seg.length
            if (position in pos until pos + size) return seg.weights.getOrNull(position - pos)
            pos += size
        }
        return null
    }

    fun space(): Double {
        var n = 1.0
        for (seg in segments) if (!seg.fixed) {
            for (w in seg.weights) n = (n * w.chars.size).coerceAtMost(1e18)
        }
        return n
    }

    fun describe(): String = buildString {
        append(template).append('\n')
        segments.forEach { seg ->
            if (seg.fixed) append("фикс: ").append(seg.text)
            else append("перем: ").append(seg.length).append(" [").append(seg.weights.firstOrNull()?.alphabet()?.take(24) ?: "").append(']')
            append('\n')
        }
    }

    companion object {
        fun analyze(keys: List<String>): KeyStructure? {
            val clean = keys.map { it.trim() }.filter { it.isNotBlank() }.distinct()
            if (clean.isEmpty()) return null
            val byLength = clean.groupBy { it.length }
            val modal = byLength.values.maxByOrNull { it.size }!!
            val use = if (modal.size >= 2) modal else listOf(clean.minBy { it.length })
            val n = use.minOf { it.length }
            val rows = use.map { it.substring(0, n) }
            if (n == 0) return null

            val firstVar = (0 until n).firstOrNull { i -> rows.any { it[i] != rows[0][i] } }
                ?: (n - 14).coerceAtLeast(0)

            val isVar = BooleanArray(n) { i ->
                i >= firstVar && rows.all { it[i].isLetterOrDigit() }
            }

            val segments = mutableListOf<KeySegment>()
            var i = 0
            while (i < n) {
                val v = isVar[i]
                var j = i + 1
                while (j < n && isVar[j] == v) j++
                if (v) {
                    val observed = (i until j).map { pos -> rows.map { it[pos] }.distinct().joinToString("") }
                    val observedAll = observed.joinToString("")
                    val family = charsetFamily(observedAll)
                    val weights = observed.map { CharSetWeights.learned(it, family) }
                    segments.add(KeySegment(false, length = j - i, weights = weights))
                } else {
                    segments.add(KeySegment(true, text = rows[0].substring(i, j)))
                }
                i = j
            }
            val template = buildString {
                segments.forEach { append(if (it.fixed) it.text else "?".repeat(it.length)) }
            }
            return KeyStructure(segments, rows, template)
        }

        fun fromTemplate(prefix: String, suffix: String, length: Int, charset: String): KeyStructure? {
            if (length <= 0 || charset.isEmpty()) return null
            val w = CharSetWeights.uniform(charset)
            return KeyStructure(
                listOf(
                    KeySegment(true, text = prefix),
                    KeySegment(false, length = length, weights = List(length) { w }),
                    KeySegment(true, text = suffix)
                ),
                emptyList(),
                prefix + "?".repeat(length) + suffix
            )
        }
    }
}
