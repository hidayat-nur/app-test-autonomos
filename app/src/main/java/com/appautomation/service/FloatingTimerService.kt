package com.appautomation.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.TextView
import com.appautomation.R
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class FloatingTimerService : Service() {

    companion object {
        private const val TAG = "FloatingTimer"
        const val ACTION_SHOW = "com.appautomation.SHOW_TIMER"
        const val ACTION_HIDE = "com.appautomation.HIDE_TIMER"
    }

    @Inject
    lateinit var automationManager: AutomationManager

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    private var timerText: TextView? = null
    private var appNameText: TextView? = null
    private var pauseButton: TextView? = null
    private var stopButton: TextView? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Log to Crashlytics
        FirebaseCrashlytics.getInstance().log("FloatingTimer onStartCommand - Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        
        android.util.Log.d(TAG, "onStartCommand action=${intent?.action}")
        android.util.Log.d(TAG, "Overlay permission=${android.provider.Settings.canDrawOverlays(this)}")
        
        when (intent?.action) {
            ACTION_SHOW -> {
                // Check if permission granted
                if (!android.provider.Settings.canDrawOverlays(this)) {
                    Log.e(TAG, "❌ SYSTEM_ALERT_WINDOW permission not granted!")
                    
                    // Report to Crashlytics
                    FirebaseCrashlytics.getInstance().apply {
                        setCustomKey("error_location", "FloatingTimer.onStartCommand")
                        setCustomKey("overlay_permission", false)
                        log("Overlay permission denied on ${Build.MANUFACTURER} ${Build.MODEL}")
                    }
                    
                    stopSelf()
                    return START_NOT_STICKY
                }
                
                if (floatingView == null) {
                    try {
                        showFloatingBubble()
                        startObservingAutomation()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to show floating bubble", e)
                        
                        // Report to Crashlytics
                        FirebaseCrashlytics.getInstance().apply {
                            setCustomKey("error_location", "FloatingTimer.showFloatingBubble")
                            recordException(e)
                        }
                    }
                }
            }
            ACTION_HIDE -> {
                hideFloatingBubble()
                stopSelf()
            }
        }
        return START_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showFloatingBubble() {
        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

            // Inflate layout
            floatingView = LayoutInflater.from(this).inflate(R.layout.floating_timer_bubble, null)

            // Find views
            timerText = floatingView?.findViewById(R.id.timer_text)
            appNameText = floatingView?.findViewById(R.id.app_name_text)
            pauseButton = floatingView?.findViewById(R.id.pause_button)
            stopButton = floatingView?.findViewById(R.id.stop_button)

            // Setup click listeners
            pauseButton?.setOnClickListener {
                val currentState = automationManager.automationState.value
                if (currentState is AutomationManager.AutomationState.Paused) {
                    automationManager.resumeAutomation()
                    pauseButton?.text = "⏸️"
                } else {
                    automationManager.pauseAutomation()
                    pauseButton?.text = "▶️"
                }
            }

            stopButton?.setOnClickListener {
                automationManager.stopAutomation()
                hideFloatingBubble()
                stopSelf()
            }

            // Window params - positioned at top right, avoiding gesture area
            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            )

            params.gravity = Gravity.TOP or Gravity.END
            // Convert dp values to pixels for proper positioning on various screens
            val metrics = resources.displayMetrics
            params.x = (16 * metrics.density).toInt()
            params.y = (100 * metrics.density).toInt()

            // Make draggable
            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f

            floatingView?.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (initialTouchX - event.rawX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(floatingView, params)
                        true
                    }
                    else -> false
                }
            }

            // Add to window
            windowManager?.addView(floatingView, params)
            Log.d(TAG, "✅ Floating bubble shown - params=(x=${params.x}, y=${params.y})")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to show floating bubble", e)
        }
    }

    private fun hideFloatingBubble() {
        try {
            floatingView?.let {
                try {
                    if (it.isAttachedToWindow) {
                        windowManager?.removeView(it)
                        android.util.Log.d(TAG, "Floating bubble removed from window (hide)")
                    } else {
                        android.util.Log.d(TAG, "Floating bubble not attached to window; skipping remove")
                    }
                } catch (inner: Exception) {
                    android.util.Log.e(TAG, "Exception while removing floating view", inner)
                }
                floatingView = null
            }
            Log.d(TAG, "✅ Floating bubble hidden")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hide floating bubble", e)
        }
    }

    private fun startObservingAutomation() {
        serviceScope.launch {
            var seenRunning = false
            automationManager.automationState.collectLatest { state ->
                when (state) {
                    is AutomationManager.AutomationState.Running -> {
                        seenRunning = true
                        val remaining = formatTime(state.remainingTimeMillis)
                        timerText?.text = remaining
                        appNameText?.text = state.currentApp.appName
                        pauseButton?.text = "⏸️"
                    }
                    is AutomationManager.AutomationState.Paused -> {
                        if (seenRunning) pauseButton?.text = "▶️"
                    }
                    is AutomationManager.AutomationState.Completed,
                    is AutomationManager.AutomationState.Error,
                    is AutomationManager.AutomationState.Idle -> {
                        if (seenRunning) {
                            hideFloatingBubble()
                            stopSelf()
                        } else {
                            android.util.Log.d(TAG, "Ignoring state=$state because bubble hasn't seen a Running state yet")
                        }
                    }
                }
            }
        }
    }

    private fun formatTime(millis: Long): String {
        val totalSeconds = (millis / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        hideFloatingBubble()
    }
}
