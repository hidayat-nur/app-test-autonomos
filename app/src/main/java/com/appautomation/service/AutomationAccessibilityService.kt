package com.appautomation.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.util.DisplayMetrics
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.*
import kotlin.random.Random

class AutomationAccessibilityService : AccessibilityService() {
    
    companion object {
        private const val TAG = "AccessibilityService"
        
        @Volatile
        private var instance: AutomationAccessibilityService? = null
        
        fun getInstance(): AutomationAccessibilityService? = instance
        
        fun isServiceEnabled(): Boolean = instance != null
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var gestureJob: Job? = null
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Accessibility service connected")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Monitor events if needed
        event?.let {
            if (it.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                Log.d(TAG, "Window changed: ${it.packageName}")
            }
        }
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        gestureJob?.cancel()
        serviceScope.cancel()
        Log.d(TAG, "Accessibility service destroyed")
    }
    
    /**
     * Perform a click gesture at specific coordinates
     */
    fun performClick(x: Float, y: Float, duration: Long = 100): Boolean {
        return try {
            val path = Path().apply { moveTo(x, y) }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
                .build()
            
            dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    Log.d(TAG, "Click gesture completed at ($x, $y)")
                }
                
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    Log.w(TAG, "Click gesture cancelled")
                }
            }, null)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform click", e)
            false
        }
    }
    
    /**
     * Perform a swipe gesture
     */
    fun performSwipe(
        startX: Float, startY: Float,
        endX: Float, endY: Float,
        duration: Long = 300
    ): Boolean {
        return try {
            val path = Path().apply {
                moveTo(startX, startY)
                lineTo(endX, endY)
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
                .build()
            
            dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    Log.d(TAG, "Swipe gesture completed")
                }
                
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    Log.w(TAG, "Swipe gesture cancelled")
                }
            }, null)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform swipe", e)
            false
        }
    }
    
    /**
     * Scroll down the screen
     */
    fun scrollDown() {
        val displayMetrics: DisplayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val screenWidth = displayMetrics.widthPixels
        
        performSwipe(
            screenWidth / 2f,
            screenHeight * 0.7f,
            screenWidth / 2f,
            screenHeight * 0.3f,
            duration = 400
        )
    }
    
    /**
     * Scroll up the screen
     */
    fun scrollUp() {
        val displayMetrics: DisplayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val screenWidth = displayMetrics.widthPixels
        
        performSwipe(
            screenWidth / 2f,
            screenHeight * 0.3f,
            screenWidth / 2f,
            screenHeight * 0.7f,
            duration = 400
        )
    }
    
    /**
     * Perform random interactions (clicks and scrolls)
     */
    fun startRandomInteractions(intervalSeconds: Int = 15) {
        stopRandomInteractions()
        
        gestureJob = serviceScope.launch {
            while (isActive) {
                delay(intervalSeconds * 1000L)
                performRandomGesture()
            }
        }
    }
    
    /**
     * Stop random interactions
     */
    fun stopRandomInteractions() {
        gestureJob?.cancel()
        gestureJob = null
    }
    
    /**
     * Perform a random gesture (tap or scroll)
     */
    private fun performRandomGesture() {
        val displayMetrics: DisplayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val screenWidth = displayMetrics.widthPixels
        
        val random = Random.Default
        val gestureType = random.nextInt(3) // 0: tap, 1: scroll down, 2: scroll up
        
        when (gestureType) {
            0 -> {
                // Random tap in safe zone (avoid edges and status bar)
                val x = random.nextInt(screenWidth / 4, screenWidth * 3 / 4).toFloat()
                val y = random.nextInt(screenHeight / 4, screenHeight * 3 / 4).toFloat()
                performClick(x, y)
                Log.d(TAG, "Random tap at ($x, $y)")
            }
            1 -> {
                scrollDown()
                Log.d(TAG, "Random scroll down")
            }
            2 -> {
                scrollUp()
                Log.d(TAG, "Random scroll up")
            }
        }
    }
    
    /**
     * Press back button
     */
    fun pressBack(): Boolean {
        return try {
            performGlobalAction(GLOBAL_ACTION_BACK)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to press back", e)
            false
        }
    }
    
    /**
     * Press home button
     */
    fun pressHome(): Boolean {
        return try {
            performGlobalAction(GLOBAL_ACTION_HOME)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to press home", e)
            false
        }
    }
    
    /**
     * Press recent apps button
     */
    fun pressRecents(): Boolean {
        return try {
            performGlobalAction(GLOBAL_ACTION_RECENTS)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to press recents", e)
            false
        }
    }
}
