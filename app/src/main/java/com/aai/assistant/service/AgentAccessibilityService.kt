package com.aai.assistant.service

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.aai.assistant.tools.AgentAccessibilityAccess
import com.aai.assistant.tools.ScreenBridge
import com.aai.assistant.tools.ScreenNodeRegistry
import com.aai.assistant.tools.ScreenStateParser
import com.aai.assistant.tools.clickGesture
import com.aai.assistant.tools.dispatchGestureAsync
import com.aai.assistant.tools.setTextOnNode
import com.aai.assistant.tools.swipeGesture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AgentAccessibilityService : AccessibilityService(), AgentAccessibilityAccess {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var parser: ScreenStateParser

    override fun onServiceConnected() {
        super.onServiceConnected()
        parser = ScreenStateParser(this)
        ScreenBridge.service = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        ScreenBridge.service = null
        scope.cancel()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        ScreenBridge.service = null
        scope.cancel()
        super.onDestroy()
    }

    override fun dumpScreen(): String = parser.dump()

    override fun tap(x: Float, y: Float, callback: (Boolean) -> Unit) {
        scope.launch { callback(dispatchGestureAsync(this@AgentAccessibilityService, clickGesture(x, y, 50))) }
    }

    override fun longPress(x: Float, y: Float, durationMs: Long, callback: (Boolean) -> Unit) {
        scope.launch { callback(dispatchGestureAsync(this@AgentAccessibilityService, clickGesture(x, y, durationMs))) }
    }

    override fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long, callback: (Boolean) -> Unit) {
        scope.launch {
            callback(dispatchGestureAsync(this@AgentAccessibilityService, swipeGesture(x1, y1, x2, y2, durationMs)))
        }
    }

    override fun typeText(text: String, nodeId: Int?): String {
        val target: AccessibilityNodeInfo? = if (nodeId != null) {
            findNodeById(nodeId)
        } else {
            rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        }
        if (target == null) return "Ошибка: не найдено текстовое поле (node_id=$nodeId). Сначала вызови get_screen_state и тапни по полю."
        if (!target.isFocused) {
            target.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        if (setTextOnNode(target, text)) {
            return "Текст введён через ACTION_SET_TEXT (${text.length} символов)"
        }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("aai", text))
        val pasted = target.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        return if (pasted) "Текст вставлен через буфер обмена (fallback)"
        else "Ошибка: поле блокирует и SET_TEXT, и PASTE"
    }

    override fun globalAction(action: String): Boolean {
        val code = when (action.uppercase()) {
            "BACK" -> GLOBAL_ACTION_BACK
            "HOME" -> GLOBAL_ACTION_HOME
            "RECENTS" -> GLOBAL_ACTION_RECENTS
            "NOTIFICATIONS" -> GLOBAL_ACTION_NOTIFICATIONS
            "QUICK_SETTINGS" -> GLOBAL_ACTION_QUICK_SETTINGS
            "POWER_DIALOG" -> GLOBAL_ACTION_POWER_DIALOG
            else -> return false
        }
        return performGlobalAction(code)
    }

    override fun findNodeById(id: Int): AccessibilityNodeInfo? = ScreenNodeRegistry.get(id)
}