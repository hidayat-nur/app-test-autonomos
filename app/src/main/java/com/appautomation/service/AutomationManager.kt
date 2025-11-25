package com.appautomation.service

import android.util.Log
import com.appautomation.data.model.AppTask
import com.appautomation.data.model.AutomationLog
import com.appautomation.data.repository.AppRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutomationManager @Inject constructor(
    private val appLauncher: AppLauncher,
    private val appMonitor: AppMonitor,
    private val repository: AppRepository
) {
    
    companion object {
        private const val TAG = "AutomationManager"
        private const val LAUNCH_GRACE_PERIOD = 3000L // 3 seconds for app to open
        private const val MAX_LAUNCH_RETRIES = 3
        private const val INTERACTION_INTERVAL_SECONDS = 15
    }
    
    sealed class AutomationState {
        object Idle : AutomationState()
        data class Running(
            val currentApp: AppTask,
            val remainingTimeMillis: Long,
            val elapsedTimeMillis: Long,
            val queue: List<AppTask>,
            val completedCount: Int,
            val totalCount: Int
        ) : AutomationState()
        object Paused : AutomationState()
        data class Completed(val totalApps: Int) : AutomationState()
        data class Error(val message: String) : AutomationState()
    }
    
    private val _automationState = MutableStateFlow<AutomationState>(AutomationState.Idle)
    val automationState: StateFlow<AutomationState> = _automationState.asStateFlow()
    
    private var automationJob: Job? = null
    private val automationScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private var isPaused = false
    private var pausedApp: AppTask? = null
    private var pausedRemainingTime: Long = 0
    private var sessionStartTime: Long = 0
    private var pausedSessionStartTime: Long = 0
    
    /**
     * Start automation with list of apps
     */
    fun startAutomation(apps: List<AppTask>) {
        if (apps.isEmpty()) {
            Log.w(TAG, "No apps to automate")
            return
        }
        
        if (!appMonitor.hasUsageStatsPermission()) {
            _automationState.value = AutomationState.Error("Usage stats permission not granted")
            return
        }
        
        stopAutomation() // Stop any existing automation
        
        automationJob = automationScope.launch {
            try {
                runAutomation(apps)
            } catch (e: CancellationException) {
                Log.d(TAG, "Automation cancelled")
                if (!isPaused) {
                    _automationState.value = AutomationState.Idle
                }
            } catch (e: Exception) {
                Log.e(TAG, "Automation error", e)
                _automationState.value = AutomationState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * Main automation loop
     */
    private suspend fun CoroutineScope.runAutomation(apps: List<AppTask>) {
        var completedCount = 0
        val totalCount = apps.size
        
        // Initialize session start time or restore from pause
        if (sessionStartTime == 0L || !isPaused) {
            sessionStartTime = System.currentTimeMillis()
        } else {
            // Restore from pause - adjust start time to maintain elapsed time
            sessionStartTime = pausedSessionStartTime
        }
        
        for ((index, appTask) in apps.withIndex()) {
            if (!isActive) break
            
            Log.d(TAG, "Starting automation for ${appTask.appName} (${index + 1}/$totalCount)")
            
            // Launch the app
            val launched = launchAppWithRetry(appTask.packageName, MAX_LAUNCH_RETRIES)
            if (!launched) {
                val errorMsg = "Failed to launch ${appTask.appName}"
                Log.e(TAG, errorMsg)
                
                // Log the failure
                repository.logAutomation(
                    AutomationLog(
                        timestamp = System.currentTimeMillis(),
                        appPackage = appTask.packageName,
                        appName = appTask.appName,
                        durationMillis = 0,
                        success = false,
                        errorMessage = errorMsg
                    )
                )
                
                _automationState.value = AutomationState.Error(errorMsg)
                return
            }
            
            // Start random interactions if accessibility service is enabled
            val accessibilityService = AutomationAccessibilityService.getInstance()
            if (accessibilityService != null) {
                accessibilityService.startRandomInteractions(500, appTask.packageName) // Pass package name for monitoring
                Log.d(TAG, "✅ Random interactions started (every 500ms) for ${appTask.packageName}")
            } else {
                Log.e(TAG, "❌ CRITICAL: Accessibility service not available!")
                Log.e(TAG, "❌ Service instance is NULL - gestures will NOT work!")
                Log.e(TAG, "❌ Please enable Accessibility Service in Settings")
            }
            
            // Run timer with countdown
            val startTime = System.currentTimeMillis()
            val endTime = startTime + appTask.durationMillis
            
            try {
                while (System.currentTimeMillis() < endTime && isActive) {
                    val remaining = endTime - System.currentTimeMillis()
                    val elapsed = System.currentTimeMillis() - sessionStartTime
                    
                    _automationState.value = AutomationState.Running(
                        currentApp = appTask,
                        remainingTimeMillis = remaining.coerceAtLeast(0),
                        elapsedTimeMillis = elapsed,
                        queue = apps.drop(index + 1),
                        completedCount = completedCount,
                        totalCount = totalCount
                    )
                    
                    delay(1000) // Update every second
                }
                
                // Stop interactions
                accessibilityService?.stopRandomInteractions()
                
                if (isActive) {
                    completedCount++
                    
                    // Log success
                    repository.logAutomation(
                        AutomationLog(
                            timestamp = System.currentTimeMillis(),
                            appPackage = appTask.packageName,
                            appName = appTask.appName,
                            durationMillis = appTask.durationMillis,
                            success = true
                        )
                    )
                    
                    Log.d(TAG, "Completed ${appTask.appName}")
                }
            } catch (e: CancellationException) {
                accessibilityService?.stopRandomInteractions()
                throw e
            }
            
            // Small delay between apps
            if (index < apps.size - 1) {
                delay(1000)
            }
        }
        
        if (isActive) {
            _automationState.value = AutomationState.Completed(completedCount)
            Log.d(TAG, "Automation completed: $completedCount/$totalCount apps")

            // NOTE: Do NOT send user to Home here. Keep app in control so Foreground
            // service (or UI) can bring our app to foreground and show completion.
        }
    }
    
    /**
     * Launch app with retry logic
     */
    private suspend fun launchAppWithRetry(packageName: String, maxRetries: Int): Boolean {
        repeat(maxRetries) { attempt ->
            Log.d(TAG, "Launching $packageName (attempt ${attempt + 1}/$maxRetries)")
            
            if (appLauncher.launchApp(packageName)) {
                delay(LAUNCH_GRACE_PERIOD)
                
                // Verify app is in foreground
                if (appMonitor.isAppInForeground(packageName)) {
                    Log.d(TAG, "$packageName is now in foreground")
                    return true
                } else {
                    Log.w(TAG, "$packageName not in foreground after launch")
                }
            }
            
            // Exponential backoff
            if (attempt < maxRetries - 1) {
                delay(1000L * (attempt + 1))
            }
        }
        
        return false
    }
    
    /**
     * Pause automation
     */
    fun pauseAutomation() {
        if (_automationState.value is AutomationState.Running) {
            val currentState = _automationState.value as AutomationState.Running
            pausedApp = currentState.currentApp
            pausedRemainingTime = currentState.remainingTimeMillis
            pausedSessionStartTime = sessionStartTime
            isPaused = true
            
            automationJob?.cancel()
            
            // Stop interactions
            AutomationAccessibilityService.getInstance()?.stopRandomInteractions()
            
            _automationState.value = AutomationState.Paused
            Log.d(TAG, "Automation paused")
        }
    }
    
    /**
     * Resume automation
     */
    fun resumeAutomation() {
        if (_automationState.value is AutomationState.Paused && pausedApp != null) {
            val app = pausedApp!!.copy(durationMillis = pausedRemainingTime)
            isPaused = false
            
            // Continue with remaining apps
            val currentState = _automationState.value as? AutomationState.Paused
            // For simplicity, restart the current app
            startAutomation(listOf(app))
            
            Log.d(TAG, "Automation resumed")
        }
    }
    
    /**
     * Stop automation completely
     */
    fun stopAutomation() {
        automationJob?.cancel()
        automationJob = null
        isPaused = false
        pausedApp = null
        pausedRemainingTime = 0
        
        // Stop interactions
        AutomationAccessibilityService.getInstance()?.stopRandomInteractions()
        
        if (_automationState.value !is AutomationState.Idle) {
            _automationState.value = AutomationState.Idle
            Log.d(TAG, "Automation stopped")
        }
    }
    
    /**
     * Check if automation is running
     */
    fun isRunning(): Boolean {
        return _automationState.value is AutomationState.Running
    }
    
    /**
     * Get current state
     */
    fun getCurrentState(): AutomationState {
        return _automationState.value
    }
}
