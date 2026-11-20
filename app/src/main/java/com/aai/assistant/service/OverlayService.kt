package com.aai.assistant.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.aai.assistant.MainActivity
import com.aai.assistant.R
import com.aai.assistant.ui.theme.AaiTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.abs

class OverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private var bubbleView: ComposeView? = null
    private var panelHost: OverlayPanelHost? = null
    private var panelParams: WindowManager.LayoutParams? = null

    private val compactFlow = MutableStateFlow(false)
    private val handler = Handler(Looper.getMainLooper())
    private var lastInteraction = 0L

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    private val autoCollapseChecker = object : Runnable {
        override fun run() {
            val host = panelHost
            if (host != null && !compactFlow.value && !host.isRunning() &&
                System.currentTimeMillis() - lastInteraction > IDLE_COLLAPSE_MS
            ) {
                setCompact(true)
            }
            handler.postDelayed(this, 1500)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startAsForeground()
        if (Settings.canDrawOverlays(this)) {
            showBubble()
        }
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        handler.post(autoCollapseChecker)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_PANEL -> showPanel()
            ACTION_HIDE -> hidePanel()
        }
        return START_STICKY
    }

    private fun startAsForeground() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "aai_overlay"
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.overlay_channel),
                NotificationManager.IMPORTANCE_MIN
            )
            manager.createNotificationChannel(channel)
        }
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

    private fun screenWidth(): Int = resources.displayMetrics.widthPixels

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun showBubble() {
        if (bubbleView != null) return
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setContent {
                AaiTheme {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .shadow(6.dp, CircleShape)
                            .clip(CircleShape)
                            .background(androidx.compose.ui.graphics.Color(0xFF2A2450)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.SmartToy,
                            contentDescription = "aAi ассистент",
                            tint = androidx.compose.ui.graphics.Color(0xFF8B7CFF),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = dp(160)
        }
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var moved = false
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = params.x
                    startY = params.y
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (abs(dx) > 8 || abs(dy) > 8) moved = true
                    if (moved) {
                        params.x = startX + dx.toInt()
                        params.y = startY + dy.toInt()
                        windowManager.updateViewLayout(view, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) togglePanel()
                    true
                }
                else -> false
            }
        }
        windowManager.addView(view, params)
        bubbleView = view
    }

    private fun togglePanel() {
        if (panelHost != null) hidePanel() else showPanel()
    }

    fun showPanel() {
        if (panelHost != null) {
            setCompact(false)
            touch()
            return
        }
        if (!Settings.canDrawOverlays(this)) return
        touch()
        compactFlow.value = false
        val host = OverlayPanelHost(
            context = this,
            compactFlow = compactFlow,
            onToggleCompact = {
                touch()
                setCompact(!compactFlow.value)
            },
            onClose = { hidePanel() },
            onDrag = { dx, dy ->
                touch()
                if (compactFlow.value) setCompact(false)
                movePanel(dx, dy)
            },
            onOpenApp = {
                startActivity(
                    Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                hidePanel()
            }
        )
        val width = (screenWidth() * 0.92f).toInt()
        val params = WindowManager.LayoutParams(
            width,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.START
            x = (screenWidth() - width) / 2
            y = dp(24)
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        windowManager.addView(host.view, params)
        panelHost = host
        panelParams = params
    }

    private fun movePanel(dx: Float, dy: Float) {
        val params = panelParams ?: return
        val host = panelHost ?: return
        params.flags = params.flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        params.x = (params.x + dx.toInt()).coerceIn(-dp(16), screenWidth() - dp(80))
        params.y = (params.y - dy.toInt()).coerceIn(dp(8), resources.displayMetrics.heightPixels - dp(120))
        runCatching { windowManager.updateViewLayout(host.view, params) }
    }

    private fun setCompact(compact: Boolean) {
        compactFlow.value = compact
        val params = panelParams ?: return
        val host = panelHost ?: return
        if (compact) {
            val width = (screenWidth() * 0.68f).toInt()
            params.width = width
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            params.x = screenWidth() - width + dp(14)
        } else {
            val width = (screenWidth() * 0.92f).toInt()
            params.width = width
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS.inv()
            params.x = params.x.coerceIn(dp(4), screenWidth() - width - dp(4))
            params.y = params.y.coerceAtLeast(dp(8))
            touch()
        }
        runCatching { windowManager.updateViewLayout(host.view, params) }
    }

    private fun touch() {
        lastInteraction = System.currentTimeMillis()
    }

    fun hidePanel() {
        panelHost?.let {
            runCatching { windowManager.removeView(it.view) }
            it.destroy()
        }
        panelHost = null
        panelParams = null
    }

    override fun onDestroy() {
        handler.removeCallbacks(autoCollapseChecker)
        hidePanel()
        bubbleView?.let { runCatching { windowManager.removeView(it) } }
        bubbleView = null
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
        super.onDestroy()
    }

    companion object {
        const val ACTION_SHOW_PANEL = "com.aai.assistant.SHOW_PANEL"
        const val ACTION_HIDE = "com.aai.assistant.HIDE_PANEL"
        private const val NOTIF_ID = 42
        private const val IDLE_COLLAPSE_MS = 8000L
    }
}