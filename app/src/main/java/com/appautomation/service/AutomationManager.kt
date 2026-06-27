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
    private var pausedSession: PausedSession? = null
    private var sessionStartTime: Long = 0
    
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

        // Fresh run: start counts at zero, full app list, new session clock.
        launchRun(apps, startCompletedCount = 0, totalCount = apps.size, sessionStart = System.currentTimeMillis())
    }

    /**
     * Launch the automation coroutine. Used by both a fresh start and a resume;
     * the difference is purely in the counts/session-clock passed in.
     */
    private fun launchRun(apps: List<AppTask>, startCompletedCount: Int, totalCount: Int, sessionStart: Long) {
        automationJob = automationScope.launch {
            try {
                runAutomation(apps, startCompletedCount, totalCount, sessionStart)
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
    private suspend fun CoroutineScope.runAutomation(
        apps: List<AppTask>,
        startCompletedCount: Int,
        totalCount: Int,
        sessionStart: Long
    ) {
        var completedCount = startCompletedCount

        // Session clock is decided by the caller (fresh start vs resume).
        sessionStartTime = sessionStart

        for ((index, appTask) in apps.withIndex()) {
            if (!isActive) break

            val appStartTime = System.currentTimeMillis()
            var accessibilityService: AutomationAccessibilityService? = null

            try {
                Log.d(TAG, "Starting automation for ${appTask.appName} (${index + 1}/$totalCount)")

                // Launch the app
                val launched = launchAppWithRetry(appTask.packageName, MAX_LAUNCH_RETRIES)
                if (!launched) {
                    val errorMsg = "Failed to launch ${appTask.appName}"
                    Log.e(TAG, errorMsg)

                    // Log the failure but DO NOT stop automation
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

                    Log.w(TAG, "⏭️ Skipping ${appTask.appName}, continuing to next app...")
                    continue // Move to next app instead of stopping
                }

                // Start random interactions if accessibility service is enabled
                accessibilityService = AutomationAccessibilityService.getInstance()
                if (accessibilityService != null) {
                    accessibilityService.startRandomInteractions(500, appTask.packageName)
                    Log.d(TAG, "✅ Random interactions started (every 500ms) for ${appTask.packageName}")
                } else {
                    Log.e(TAG, "❌ CRITICAL: Accessibility service not available!")
                    Log.e(TAG, "❌ Service instance is NULL - gestures will NOT work!")
                    Log.e(TAG, "❌ Please enable Accessibility Service in Settings")
                }

                // Run timer with countdown - MUST complete regardless of app state
                val endTime = appStartTime + appTask.durationMillis

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

                // Stop interactions after timer completes
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

                    Log.d(TAG, "✅ Completed ${appTask.appName}")
                }

            } catch (e: CancellationException) {
                // Only re-throw cancellation (user stopped automation)
                accessibilityService?.stopRandomInteractions()
                throw e
            } catch (e: Exception) {
                // Catch ANY other exception - log it but continue to next app
                Log.e(TAG, "❌ Error during automation for ${appTask.appName}", e)
                accessibilityService?.stopRandomInteractions()

                repository.logAutomation(
                    AutomationLog(
                        timestamp = System.currentTimeMillis(),
                        appPackage = appTask.packageName,
                        appName = appTask.appName,
                        durationMillis = System.currentTimeMillis() - appStartTime,
                        success = false,
                        errorMessage = e.message ?: "Runtime error"
                    )
                )

                Log.w(TAG, "⏭️ Error handled, continuing to next app...")
                // Do NOT break or return - continue to next app
            }

            // Small delay between apps
            if (index < apps.size - 1 && isActive) {
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
        val currentState = _automationState.value
        if (currentState is AutomationState.Running) {
            // B1: capture the FULL remaining queue (current app + everything after it)
            // so resume can continue the whole run instead of just the current app.
            pausedSession = PausedSession(
                currentApp = currentState.currentApp,
                remainingTimeMillis = currentState.remainingTimeMillis,
                remainingQueue = currentState.queue,
                completedCount = currentState.completedCount,
                totalCount = currentState.totalCount,
                elapsedTimeMillis = currentState.elapsedTimeMillis
            )
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
        val paused = pausedSession
        if (_automationState.value is AutomationState.Paused && paused != null) {
            val plan = buildResumePlan(paused, System.currentTimeMillis())
            isPaused = false
            pausedSession = null

            // Cancel any lingering job WITHOUT going through stopAutomation() (which
            // would wipe the saved session); then relaunch the full remaining queue.
            automationJob?.cancel()
            launchRun(plan.apps, plan.startCompletedCount, plan.totalCount, plan.sessionStartTime)

            Log.d(TAG, "Automation resumed with ${plan.apps.size} app(s) remaining")
        }
    }
    
    /**
     * Stop automation completely
     */
    fun stopAutomation() {
        automationJob?.cancel()
        automationJob = null
        isPaused = false
        pausedSession = null

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
