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
        
        // Browser and external app blacklist
        private val BROWSER_PACKAGES = setOf(
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.dev",
            "org.mozilla.firefox",
            "com.opera.browser",
            "com.opera.mini.native",
            "com.brave.browser",
            "com.microsoft.emmx",
            "com.UCMobile.intl",
            "com.kiwibrowser.browser",
            "com.duckduckgo.mobile.android",
            "org.chromium.chrome",
            "com.sec.android.app.sbrowser", // Samsung Internet
            "mark.via.gp",
            "com.amazon.cloud9",
            "com.android.browser"
        )
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var gestureJob: Job? = null
    private var gestureCount = 0
    private var currentTargetPackage: String? = null
    private var currentActivePackage: String? = null
    private var isGesturePaused = false
    private var lastRelaunchTime = 0L
    private var relaunchJob: Job? = null
    private var monitorJob: Job? = null
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Accessibility service connected")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            if (it.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                val packageName = it.packageName?.toString()
                currentActivePackage = packageName
                
                Log.d(TAG, "Window changed: $packageName (target: $currentTargetPackage)")
                
                // CRITICAL: Check if it's a BROWSER!
                if (packageName != null && BROWSER_PACKAGES.contains(packageName)) {
                    Log.e(TAG, "🚫 BROWSER DETECTED: $packageName - BLOCKING NOW!")
                    isGesturePaused = true
                    
                    // IMMEDIATE ACTION - Exit browser AND relaunch target app!
                    serviceScope.launch {
                        // BACK to exit browser
                        Log.d(TAG, "⬅️ Pressing BACK to exit browser")
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        delay(200)
                        
                        // IMMEDIATELY relaunch target app (don't wait to check)
                        if (currentTargetPackage != null) {
                            Log.w(TAG, "🔄 RELAUNCH target app: $currentTargetPackage")
                            relaunchtargetApp()
                            
                            // Resume gestures after relaunch
                            delay(500)
                            isGesturePaused = false
                            Log.d(TAG, "▶️ RESUME gestures after relaunch")
                        }
                    }
                    return
                }
                
                // Check if we're in target app or not
                if (currentTargetPackage != null && packageName != null) {
                    if (packageName == currentTargetPackage) {
                        // Back in target app - resume gestures
                        if (isGesturePaused) {
                            isGesturePaused = false
                            relaunchJob?.cancel()
                            relaunchJob = null
                            Log.d(TAG, "✅ Back in target app - resuming gestures")
                        }
                    } else if (!packageName.startsWith("com.android.systemui")) {
                        // Switched to different app (not browser, handled above)
                        if (!isGesturePaused) {
                            isGesturePaused = true
                            Log.w(TAG, "⚠️ ESCAPED TO: $packageName")
                        }
                        
                        // Debounce relaunch
                        val now = System.currentTimeMillis()
                        if (now - lastRelaunchTime > 1000) {
                            lastRelaunchTime = now
                            relaunchJob?.cancel()
                            
                            relaunchJob = serviceScope.launch {
                                // Try back button
                                performGlobalAction(GLOBAL_ACTION_BACK)
                                delay(800)
                                
                                // Force relaunch if needed
                                if (currentActivePackage != currentTargetPackage) {
                                    relaunchtargetApp()
                                }
                            }
                        }
                    }
                }
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
            
            // Fire and forget - don't wait for callback
            dispatchGesture(gesture, null, null)
            Log.d(TAG, "✅ Click dispatched at ($x, $y)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ FAILED to perform click: ${e.message}", e)
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
            
            // Fire and forget - don't wait for callback
            dispatchGesture(gesture, null, null)
            Log.d(TAG, "✅ Swipe dispatched")
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
    fun startRandomInteractions(intervalMillis: Long = 500, targetPackage: String? = null) {
        stopRandomInteractions()
        
        currentTargetPackage = targetPackage
        Log.d(TAG, "🎮 Starting random interactions (interval: ${intervalMillis}ms)")
        Log.d(TAG, "📱 Target package: $targetPackage")
        Log.d(TAG, "📱 Service instance: ${if (instance != null) "ACTIVE" else "NULL"}")
        
        gestureJob = serviceScope.launch {
            // First gesture immediately
            Log.d(TAG, "🔥 Performing FIRST gesture NOW...")
            performRandomGesture()
            
            // Then loop with interval
            while (isActive) {
                delay(intervalMillis)
                val startTime = System.currentTimeMillis()
                performRandomGesture()
                val elapsed = System.currentTimeMillis() - startTime
                Log.d(TAG, "⏱️ Gesture execution took ${elapsed}ms")
            }
        }
    }
    
    /**
     * Stop random interactions
     */
    fun stopRandomInteractions() {
        if (gestureJob != null) {
            Log.d(TAG, "🛑 Stopping random interactions (performed $gestureCount gestures)")
            gestureCount = 0
        }
        gestureJob?.cancel()
        gestureJob = null
        relaunchJob?.cancel()
        relaunchJob = null
        currentTargetPackage = null
        currentActivePackage = null
        isGesturePaused = false
        lastRelaunchTime = 0
    }
    
    /**
     * Relaunch target app when user switches away
     */
    private fun relaunchtargetApp() {
        currentTargetPackage?.let { packageName ->
            try {
                val intent = packageManager.getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                    Log.d(TAG, "🔄 Relaunched target app: $packageName")
                } else {
                    Log.e(TAG, "❌ Cannot relaunch: No launch intent for $packageName")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to relaunch app", e)
            }
        }
    }
    
    /**
     * Get current active package from accessibility tree
     */
    private fun getCurrentPackageName(): String? {
        return try {
            rootInActiveWindow?.packageName?.toString()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Perform a random gesture (tap, scroll, swipe)
     */
    private fun performRandomGesture() {
        // CRITICAL CHECK 1: Don't perform if paused
        if (isGesturePaused) {
            Log.d(TAG, "⏸️ Gesture BLOCKED - paused flag")
            return
        }
        
        // CRITICAL CHECK 2: Real-time package verification
        val currentPkg = getCurrentPackageName()
        
        // Block if it's a browser
        if (currentPkg != null && BROWSER_PACKAGES.contains(currentPkg)) {
            Log.e(TAG, "🚫 GESTURE BLOCKED - Browser detected: $currentPkg")
            isGesturePaused = true
            // Immediate exit and relaunch
            serviceScope.launch {
                performGlobalAction(GLOBAL_ACTION_BACK)
                delay(200)
                if (currentTargetPackage != null) {
                    relaunchtargetApp()
                    delay(500)
                    isGesturePaused = false
                    Log.d(TAG, "▶️ RESUME gestures after browser exit")
                }
            }
            return
        }
        
        // Block if not in target app
        if (currentPkg != null && currentPkg != currentTargetPackage) {
            Log.w(TAG, "⚠️ GESTURE BLOCKED - Not in target app: $currentPkg vs $currentTargetPackage")
            isGesturePaused = true
            serviceScope.launch {
                performGlobalAction(GLOBAL_ACTION_BACK)
                delay(500)
                if (getCurrentPackageName() != currentTargetPackage) {
                    relaunchtargetApp()
                    delay(500)
                    isGesturePaused = false
                    Log.d(TAG, "▶️ RESUME gestures after relaunch")
                }
            }
            return
        }
        
        gestureCount++
        val displayMetrics: DisplayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val screenWidth = displayMetrics.widthPixels
        
        val random = Random.Default
        val actionType = random.nextInt(100)
        
        when {
            actionType < 15 -> {
                // 15% - Try bottom navigation tabs
                if (clickBottomNavigation(screenWidth, screenHeight)) {
                    Log.d(TAG, "📱 Gesture #$gestureCount: BOTTOM NAV")
                    return
                }
            }
            actionType < 25 -> {
                // 10% - Try floating action button (FAB)
                if (clickFloatingActionButton(screenWidth, screenHeight)) {
                    Log.d(TAG, "🔘 Gesture #$gestureCount: FAB BUTTON")
                    return
                }
            }
            actionType < 35 -> {
                // 10% - Try toolbar/action bar buttons
                if (clickToolbarButtons(screenWidth, screenHeight)) {
                    Log.d(TAG, "🔝 Gesture #$gestureCount: TOOLBAR BUTTON")
                    return
                }
            }
            actionType < 50 -> {
                // 15% - Try to click any UI element
                try {
                    if (clickRandomClickableElement()) {
                        Log.d(TAG, "✅ Gesture #$gestureCount: CLICKED UI ELEMENT")
                        return
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "⚠️ UI click failed", e)
                }
            }
        }
        
        // Fallback to random gestures (50%)
        val gestureType = random.nextInt(10)
        
        when (gestureType) {
            0, 1, 2, 3 -> {
                // 40% - Tap random area (avoid top-right corner where bubble is)
                val safeX = random.nextInt((screenWidth * 0.2f).toInt(), (screenWidth * 0.7f).toInt()).toFloat()
                val safeY = random.nextInt((screenHeight * 0.25f).toInt(), (screenHeight * 0.8f).toInt()).toFloat()
                performClick(safeX, safeY)
                Log.d(TAG, "👆 Gesture #$gestureCount: TAP at (${safeX.toInt()}, ${safeY.toInt()})")
            }
            4, 5, 6 -> {
                // 30% - Scroll down
                scrollDown()
                Log.d(TAG, "👇 Gesture #$gestureCount: SCROLL DOWN")
            }
            7, 8 -> {
                // 20% - Scroll up
                scrollUp()
                Log.d(TAG, "👆 Gesture #$gestureCount: SCROLL UP")
            }
            9 -> {
                // 10% - Swipe left
                val startX = screenWidth * 0.7f
                val endX = screenWidth * 0.3f
                val y = screenHeight * 0.5f
                performSwipe(startX, y, endX, y, 200)
                Log.d(TAG, "👈 Gesture #$gestureCount: SWIPE LEFT")
            }
        }
    }
    
    /**
     * Try to click bottom navigation tabs
     */
    private fun clickBottomNavigation(screenWidth: Int, screenHeight: Int): Boolean {
        try {
            val rootNode = rootInActiveWindow ?: return false
            val bottomNodes = mutableListOf<android.view.accessibility.AccessibilityNodeInfo>()
            
            // Find clickable elements in bottom 15% of screen
            findBottomElements(rootNode, bottomNodes, screenHeight)
            
            if (bottomNodes.isEmpty()) {
                rootNode.recycle()
                return false
            }
            
            // Filter for likely navigation items
            val navNodes = bottomNodes.filter { node ->
                val className = node.className?.toString() ?: ""
                val desc = node.contentDescription?.toString()?.lowercase() ?: ""
                
                className.contains("Tab") || 
                className.contains("BottomNavigationItemView") ||
                desc.contains("tab") || desc.contains("navigation")
            }
            
            val targetNode = (if (navNodes.isNotEmpty()) navNodes else bottomNodes).randomOrNull()
            
            if (targetNode != null) {
                val bounds = Rect()
                targetNode.getBoundsInScreen(bounds)
                val text = targetNode.text?.toString() ?: targetNode.contentDescription?.toString() ?: "Nav"
                Log.d(TAG, "📱 Bottom nav: '$text'")
                
                bottomNodes.forEach { it.recycle() }
                rootNode.recycle()
                
                return performClick(bounds.centerX().toFloat(), bounds.centerY().toFloat())
            }
            
            bottomNodes.forEach { it.recycle() }
            rootNode.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "Error clicking bottom nav", e)
        }
        return false
    }
    
    /**
     * Try to click floating action button (FAB)
     */
    private fun clickFloatingActionButton(screenWidth: Int, screenHeight: Int): Boolean {
        try {
            val rootNode = rootInActiveWindow ?: return false
            val fabNodes = mutableListOf<android.view.accessibility.AccessibilityNodeInfo>()
            
            findFabElements(rootNode, fabNodes)
            
            if (fabNodes.isEmpty()) {
                rootNode.recycle()
                return false
            }
            
            val fabNode = fabNodes.firstOrNull()
            if (fabNode != null) {
                val bounds = Rect()
                fabNode.getBoundsInScreen(bounds)
                Log.d(TAG, "🔘 FAB found at (${bounds.centerX()}, ${bounds.centerY()})")
                
                fabNodes.forEach { it.recycle() }
                rootNode.recycle()
                
                return performClick(bounds.centerX().toFloat(), bounds.centerY().toFloat())
            }
            
            fabNodes.forEach { it.recycle() }
            rootNode.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "Error clicking FAB", e)
        }
        return false
    }
    
    /**
     * Try to click toolbar/action bar buttons
     */
    private fun clickToolbarButtons(screenWidth: Int, screenHeight: Int): Boolean {
        try {
            val rootNode = rootInActiveWindow ?: return false
            val toolbarNodes = mutableListOf<android.view.accessibility.AccessibilityNodeInfo>()
            
            // Find clickable elements in top 15% of screen
            findTopElements(rootNode, toolbarNodes, screenHeight)
            
            if (toolbarNodes.isEmpty()) {
                rootNode.recycle()
                return false
            }
            
            // Filter out back/close buttons, prefer action buttons
            val actionNodes = toolbarNodes.filter { node ->
                val text = node.text?.toString()?.lowercase() ?: ""
                val desc = node.contentDescription?.toString()?.lowercase() ?: ""
                
                !text.contains("back") && !text.contains("close") && 
                !desc.contains("back") && !desc.contains("navigate up") &&
                !desc.contains("close")
            }
            
            val targetNode = (if (actionNodes.isNotEmpty()) actionNodes else toolbarNodes).randomOrNull()
            
            if (targetNode != null) {
                val bounds = Rect()
                targetNode.getBoundsInScreen(bounds)
                val text = targetNode.text?.toString() ?: targetNode.contentDescription?.toString() ?: "Action"
                Log.d(TAG, "🔝 Toolbar: '$text'")
                
                toolbarNodes.forEach { it.recycle() }
                rootNode.recycle()
                
                return performClick(bounds.centerX().toFloat(), bounds.centerY().toFloat())
            }
            
            toolbarNodes.forEach { it.recycle() }
            rootNode.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "Error clicking toolbar", e)
        }
        return false
    }
    
    /**
     * Find elements in bottom area of screen
     */
    private fun findBottomElements(
        node: android.view.accessibility.AccessibilityNodeInfo,
        result: MutableList<android.view.accessibility.AccessibilityNodeInfo>,
        screenHeight: Int
    ) {
        try {
            if (result.size > 20) return
            
            if (node.isClickable && node.isVisibleToUser && node.isEnabled) {
                val bounds = Rect()
                node.getBoundsInScreen(bounds)
                
                // Bottom 15% of screen
                if (bounds.centerY() > screenHeight * 0.85) {
                    result.add(node)
                }
            }
            
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                findBottomElements(child, result, screenHeight)
            }
        } catch (e: Exception) {
            // Silent fail
        }
    }
    
    /**
     * Find floating action button elements
     */
    private fun findFabElements(
        node: android.view.accessibility.AccessibilityNodeInfo,
        result: MutableList<android.view.accessibility.AccessibilityNodeInfo>
    ) {
        try {
            if (result.size > 5) return
            
            val className = node.className?.toString() ?: ""
            
            if (node.isClickable && node.isVisibleToUser && node.isEnabled &&
                (className.contains("FloatingActionButton") || className.contains("FAB"))) {
                result.add(node)
            }
            
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                findFabElements(child, result)
            }
        } catch (e: Exception) {
            // Silent fail
        }
    }
    
    /**
     * Find elements in top area of screen (toolbar)
     */
    private fun findTopElements(
        node: android.view.accessibility.AccessibilityNodeInfo,
        result: MutableList<android.view.accessibility.AccessibilityNodeInfo>,
        screenHeight: Int
    ) {
        try {
            if (result.size > 15) return
            
            if (node.isClickable && node.isVisibleToUser && node.isEnabled) {
                val bounds = Rect()
                node.getBoundsInScreen(bounds)
                
                // Top 15% of screen
                if (bounds.centerY() < screenHeight * 0.15) {
                    result.add(node)
                }
            }
            
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                findTopElements(child, result, screenHeight)
            }
        } catch (e: Exception) {
            // Silent fail
        }
    }
    
    /**
     * Find and click a random clickable UI element
     */
    private fun clickRandomClickableElement(): Boolean {
        try {
            val rootNode = rootInActiveWindow ?: return false
            val clickableNodes = mutableListOf<android.view.accessibility.AccessibilityNodeInfo>()
            
            // Recursively find all clickable nodes (with limit to avoid slowness)
            findClickableNodes(rootNode, clickableNodes, maxDepth = 15)
            
            if (clickableNodes.isEmpty()) {
                Log.d(TAG, "⚠️ No clickable elements found")
                rootNode.recycle()
                return false
            }
            
            Log.d(TAG, "📊 Found ${clickableNodes.size} clickable elements")
            
            // Simple filter - just avoid back/close buttons
            val safeNodes = clickableNodes.filter { node ->
                val text = node.text?.toString()?.lowercase() ?: ""
                val desc = node.contentDescription?.toString()?.lowercase() ?: ""
                
                val isDangerous = text.contains("back") || text.contains("close") || 
                                 text.contains("exit") || desc.contains("back") || 
                                 desc.contains("close") || desc.contains("navigate up")
                
                !isDangerous && node.isVisibleToUser
            }
            
            val targetNodes = if (safeNodes.isNotEmpty()) safeNodes else clickableNodes.take(10)
            
            if (targetNodes.isEmpty()) {
                clickableNodes.forEach { it.recycle() }
                rootNode.recycle()
                return false
            }
            
            // Pick random element and click it
            val randomNode = targetNodes.random()
            val bounds = Rect()
            randomNode.getBoundsInScreen(bounds)
            
            val centerX = bounds.centerX().toFloat()
            val centerY = bounds.centerY().toFloat()
            
            val text = randomNode.text?.toString() ?: randomNode.contentDescription?.toString() ?: "Unknown"
            Log.d(TAG, "🎯 Clicking UI: '$text' at ($centerX, $centerY)")
            
            // Cleanup
            clickableNodes.forEach { it.recycle() }
            rootNode.recycle()
            
            return performClick(centerX, centerY)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error finding clickable: ${e.message}")
            return false
        }
    }
    
    /**
     * Recursively find all clickable nodes in the view hierarchy
     */
    private fun findClickableNodes(
        node: android.view.accessibility.AccessibilityNodeInfo,
        result: MutableList<android.view.accessibility.AccessibilityNodeInfo>,
        maxDepth: Int = 15,
        currentDepth: Int = 0
    ) {
        try {
            if (currentDepth > maxDepth) return
            if (result.size > 50) return // Max 50 elements for speed
            
            if (node.isClickable && node.isVisibleToUser && node.isEnabled) {
                result.add(node)
            }
            
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                findClickableNodes(child, result, maxDepth, currentDepth + 1)
            }
        } catch (e: Exception) {
            // Silent fail untuk speed
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
