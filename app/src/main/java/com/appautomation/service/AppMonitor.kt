package com.appautomation.service

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "AppMonitor"
    }
    
    private var lastKnownApp: String? = null
    
    /**
     * Get the currently running foreground app
     */
    fun getCurrentForegroundApp(): String? {
        if (!hasUsageStatsPermission()) {
            Log.w(TAG, "Usage stats permission not granted")
            return null
        }
        
        return try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            if (usageStatsManager == null) {
                Log.e(TAG, "UsageStatsManager is null")
                return null
            }
            
            val currentTime = System.currentTimeMillis()
            val stats: List<UsageStats> = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                currentTime - 1000 * 10, // Last 10 seconds
                currentTime
            ) ?: emptyList()
            
            val foregroundApp = stats
                .filter { it.lastTimeUsed > 0 }
                .maxByOrNull { it.lastTimeUsed }
                ?.packageName
            
            if (foregroundApp != null) {
                lastKnownApp = foregroundApp
            }
            
            foregroundApp
        } catch (e: Exception) {
            Log.e(TAG, "Error getting foreground app", e)
            lastKnownApp
        }
    }
    
    /**
     * Check if a specific app is in foreground
     */
    fun isAppInForeground(packageName: String): Boolean {
        val currentApp = getCurrentForegroundApp()
        return currentApp == packageName
    }
    
    /**
     * Check if usage stats permission is granted
     */
    fun hasUsageStatsPermission(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            if (appOps == null) {
                Log.e(TAG, "AppOpsManager is null")
                return false
            }
            
            val mode = appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            Log.e(TAG, "Error checking usage stats permission", e)
            false
        }
    }
    
    /**
     * Get last known foreground app (cached)
     */
    fun getLastKnownApp(): String? = lastKnownApp
}
