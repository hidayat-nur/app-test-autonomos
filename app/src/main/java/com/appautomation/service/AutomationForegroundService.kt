package com.appautomation.service

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.appautomation.R
import com.appautomation.presentation.ui.MainActivity
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class AutomationForegroundService : Service() {
    
    companion object {
        private const val TAG = "ForegroundService"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "automation_channel"
        const val ACTION_PAUSE = "com.appautomation.ACTION_PAUSE"
        const val ACTION_STOP = "com.appautomation.ACTION_STOP"
        const val ACTION_RESUME = "com.appautomation.ACTION_RESUME"
    }
    
    @Inject
    lateinit var automationManager: AutomationManager
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            // Log to Crashlytics
            FirebaseCrashlytics.getInstance().log("ForegroundService onCreate - Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            
            createNotificationChannel()
            startForeground(NOTIFICATION_ID, createNotification("Initializing...", 0))
            android.util.Log.d(TAG, "✅ Foreground service started")
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Failed to start foreground - Device: ${Build.MANUFACTURER} ${Build.MODEL}", e)
            // Report to Crashlytics
            FirebaseCrashlytics.getInstance().apply {
                setCustomKey("error_location", "ForegroundService.onCreate")
                setCustomKey("device_manufacturer", Build.MANUFACTURER)
                setCustomKey("device_model", Build.MODEL)
                recordException(e)
            }
            throw e
        }
        
        // Acquire wake lock to prevent device from sleeping
        acquireWakeLock()
        
        // Start floating timer bubble (optional - don't crash if fails)
        if (android.provider.Settings.canDrawOverlays(this)) {
            val floatingIntent = Intent(this, FloatingTimerService::class.java).apply {
                action = FloatingTimerService.ACTION_SHOW
            }
            try {
                startService(floatingIntent)
                android.util.Log.d(TAG, "✅ FloatingTimerService started")
            } catch (e: Exception) {
                android.util.Log.w(TAG, "⚠️ FloatingTimerService failed (non-critical)", e)
                FirebaseCrashlytics.getInstance().log("FloatingTimer failed on ${Build.MANUFACTURER} - non-critical")
            }
        } else {
            android.util.Log.d(TAG, "⚠️ Overlay permission not granted - skipping floating timer")
        }
        
        // Observe automation state and update notification
        serviceScope.launch {
            automationManager.automationState.collectLatest { state ->
                when (state) {
                    is AutomationManager.AutomationState.Running -> {
                        val progress = ((state.currentApp.durationMillis - state.remainingTimeMillis) * 100 / state.currentApp.durationMillis).toInt()
                        val timeStr = formatTime(state.remainingTimeMillis)
                        val elapsedStr = formatTime(state.elapsedTimeMillis)
                        val notification = createNotification(
                            "${state.currentApp.appName} - $timeStr remaining",
                            progress,
                            elapsedTime = elapsedStr
                        )
                        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.notify(NOTIFICATION_ID, notification)
                    }
                    is AutomationManager.AutomationState.Completed -> {
                        // No completion notification (permission may be missing).
                        // Still bring our app to foreground so user sees the Completed UI.
                        try {
                            delay(500)
                            val intent = Intent(this@AutomationForegroundService, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                            startActivity(intent)
                            android.util.Log.d(TAG, "Launched MainActivity after completion")
                        } catch (e: Exception) {
                            android.util.Log.e(TAG, "Failed to launch MainActivity on completion", e)
                        }

                        // Keep service alive briefly to ensure activity is shown, then stop
                        delay(2000)
                        stopSelf()
                    }
                    is AutomationManager.AutomationState.Error -> {
                        val notification = createNotification("Error: ${state.message}", 0)
                        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.notify(NOTIFICATION_ID, notification)
                        delay(5000)
                        stopSelf()
                    }
                    is AutomationManager.AutomationState.Paused -> {
                        val notification = createNotification("Paused", 0, isPaused = true)
                        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.notify(NOTIFICATION_ID, notification)
                    }
                    is AutomationManager.AutomationState.Idle -> {
                        // Will stop service
                    }
                }
            }
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> {
                automationManager.pauseAutomation()
            }
            ACTION_RESUME -> {
                automationManager.resumeAutomation()
            }
            ACTION_STOP -> {
                android.util.Log.d(TAG, "🛑 STOP button pressed - stopping automation")
                automationManager.stopAutomation()
                
                // Show stop notification briefly
                val stopNotification = createStopNotification()
                val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, stopNotification)
                
                // Stop service after brief delay
                serviceScope.launch {
                    delay(1000)
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Hide floating timer bubble
        val floatingIntent = Intent(this, FloatingTimerService::class.java).apply {
            action = FloatingTimerService.ACTION_HIDE
        }
        startService(floatingIntent)
        
        releaseWakeLock()
        serviceScope.cancel()
    }
    
    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "AppAutomation::AutomationWakeLock"
            ).apply {
                acquire(10 * 60 * 60 * 1000L) // 10 hours max
            }
            android.util.Log.d(TAG, "✅ Wake lock acquired - device won't sleep during automation")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Failed to acquire wake lock", e)
        }
    }
    
    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    android.util.Log.d(TAG, "✅ Wake lock released")
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Failed to release wake lock", e)
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(text: String, progress: Int, isPaused: Boolean = false, elapsedTime: String? = null): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
        
        // Add elapsed time as subtext if provided
        if (elapsedTime != null) {
            builder.setSubText(getString(R.string.notification_elapsed_time, elapsedTime))
        }
        
        if (progress > 0) {
            builder.setProgress(100, progress, false)
        }
        
        // Add action buttons
        if (isPaused) {
            val resumeIntent = Intent(this, AutomationForegroundService::class.java).apply {
                action = ACTION_RESUME
            }
            val resumePendingIntent = PendingIntent.getService(
                this, 1, resumeIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(
                R.drawable.ic_play, 
                "▶️ ${getString(R.string.resume)}", 
                resumePendingIntent
            )
        } else {
            val pauseIntent = Intent(this, AutomationForegroundService::class.java).apply {
                action = ACTION_PAUSE
            }
            val pausePendingIntent = PendingIntent.getService(
                this, 1, pauseIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(
                R.drawable.ic_pause, 
                "⏸️ ${getString(R.string.pause)}", 
                pausePendingIntent
            )
        }
        
        // STOP button - always visible and prominent
        val stopIntent = Intent(this, AutomationForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        builder.addAction(
            R.drawable.ic_stop, 
            "⛔ ${getString(R.string.stop)}", 
            stopPendingIntent
        )
        
        // Make notification persistent and high priority
        builder.setOngoing(true)
        builder.setPriority(NotificationCompat.PRIORITY_HIGH)
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        
        return builder.build()
    }
    
    private fun createStopNotification(): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BorderTech Automation")
            .setContentText("⛔ Automation Stopped")
            .setSmallIcon(R.drawable.ic_stop)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        
        return builder.build()
    }
    
    private fun formatTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}
