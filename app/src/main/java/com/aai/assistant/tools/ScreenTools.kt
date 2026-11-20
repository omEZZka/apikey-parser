package com.aai.assistant.tools

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.coroutines.resume

object ScreenBridge {
    @Volatile
    var service: AgentAccessibilityAccess? = null

    fun isConnected(): Boolean = service != null
}

interface AgentAccessibilityAccess {
    fun dumpScreen(): String
    fun tap(x: Float, y: Float, callback: (Boolean) -> Unit)
    fun longPress(x: Float, y: Float, durationMs: Long, callback: (Boolean) -> Unit)
    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long, callback: (Boolean) -> Unit)
    fun typeText(text: String, nodeId: Int?): String
    fun globalAction(action: String): Boolean
    fun findNodeById(id: Int): AccessibilityNodeInfo?
}

class ScreenStateParser(private val service: AccessibilityService) {

    private var counter = 0
    private val nodeMap = mutableMapOf<Int, AccessibilityNodeInfo>()

    fun dump(): String {
        nodeMap.values.forEach { runCatching { it.recycle() } }
        nodeMap.clear()
        counter = 0
        val root = service.rootInActiveWindow
        val elements = buildJsonArray {
            if (root != null) walk(root, this)
        }
        ScreenNodeRegistry.replace(nodeMap.toMap())
        val pkg = root?.packageName?.toString() ?: "unknown"
        runCatching { root?.recycle() }
        val result = buildJsonObject {
            put("package", pkg)
            put("element_count", elements.size)
            put("elements", elements)
        }
        return result.toString()
    }

    private fun walk(node: AccessibilityNodeInfo, array: kotlinx.serialization.json.JsonArrayBuilder) {
        if (counter >= 400) return
        val text = node.text?.toString()?.trim().orEmpty()
        val desc = node.contentDescription?.toString()?.trim().orEmpty()
        val viewId = node.viewIdResourceName?.substringAfterLast('/')?.trim().orEmpty()
        val editable = node.isEditable
        val interesting = text.isNotEmpty() || desc.isNotEmpty() ||
            node.isClickable || editable || node.isScrollable || node.isCheckable
        val bounds = Rect().also { node.getBoundsInScreen(it) }
        var assignedId = -1
        if (interesting && !bounds.isEmpty) {
            assignedId = counter++
            nodeMap[assignedId] = AccessibilityNodeInfo.obtain(node)
            val entry = buildJsonObject {
                put("id", assignedId)
                if (text.isNotEmpty()) put("text", text.take(300))
                if (desc.isNotEmpty()) put("desc", desc.take(200))
                if (viewId.isNotEmpty()) put("res", viewId)
                put("class", node.className?.toString()?.substringAfterLast('.') ?: "View")
                put("bounds", buildJsonArray {
                    add(bounds.centerX())
                    add(bounds.centerY())
                })
                if (node.isClickable) put("clickable", true)
                if (editable) put("editable", true)
                if (node.isScrollable) put("scrollable", true)
                if (node.isCheckable) put("checkable", true)
                if (node.isChecked) put("checked", true)
                if (node.isFocused) put("focused", true)
            }
            array.add(entry)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            walk(child, array)
        }
    }
}

object ScreenNodeRegistry {
    private var nodes: Map<Int, AccessibilityNodeInfo> = emptyMap()

    @Synchronized
    fun replace(map: Map<Int, AccessibilityNodeInfo>) {
        nodes.values.forEach { runCatching { it.recycle() } }
        nodes = map
    }

    @Synchronized
    fun get(id: Int): AccessibilityNodeInfo? = nodes[id]?.also { runCatching { it.refresh() } }
}

suspend fun dispatchGestureAsync(
    service: AccessibilityService,
    gesture: GestureDescription
): Boolean = kotlinx.coroutines.withTimeoutOrNull(15_000) {
    suspendCancellableCoroutine { cont ->
        val dispatched = service.dispatchGesture(
            gesture,
            object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    if (cont.isActive) cont.resume(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    if (cont.isActive) cont.resume(false)
                }
            },
            null
        )
        if (!dispatched && cont.isActive) cont.resume(false)
    }
} ?: false

fun clickGesture(x: Float, y: Float, durationMs: Long): GestureDescription {
    val path = Path().apply { moveTo(x, y) }
    val stroke = GestureDescription.StrokeDescription(path, 0, durationMs.coerceIn(1, 60_000))
    return GestureDescription.Builder().addStroke(stroke).build()
}

fun swipeGesture(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long): GestureDescription {
    val path = Path().apply {
        moveTo(x1, y1)
        lineTo(x2, y2)
    }
    val stroke = GestureDescription.StrokeDescription(path, 0, durationMs.coerceIn(1, 60_000))
    return GestureDescription.Builder().addStroke(stroke).build()
}

fun setTextOnNode(node: AccessibilityNodeInfo, text: String): Boolean {
    val args = Bundle().apply {
        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
    }
    return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
}