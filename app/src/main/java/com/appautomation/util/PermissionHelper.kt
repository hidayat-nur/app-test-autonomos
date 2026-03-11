package com.appautomation.util

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat

object PermissionHelper {
    
    /**
     * Check if Accessibility Service is enabled
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val service = "${context.packageName}/${context.packageName}.service.AutomationAccessibilityService"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(service) == true
    }
    
    /**
     * Check if Usage Stats permission is granted
     * Uses public API checkOpNoThrow (available since API 19) for maximum compatibility
     * Fixed for Motorola E6, VIVO Y71, and all custom ROMs
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            // Usage Stats not available before Android 4.4
            return false
        }
        
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            if (appOps == null) {
                android.util.Log.e("PermissionHelper", "AppOpsManager not available")
                return false
            }
            
            // Log to Crashlytics for monitoring
            try {
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().log(
                    "Checking UsageStats permission on ${Build.MANUFACTURER} ${Build.MODEL} API ${Build.VERSION.SDK_INT}"
                )
            } catch (e: Exception) {
                // Firebase not available, ignore
            }
            
            // Use checkOpNoThrow - available since API 19 (Android 4.4)
            // This is the PUBLIC API, not internal like unsafeCheckOpNoThrow
            @Suppress("DEPRECATION")
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            
            val hasPermission = mode == AppOpsManager.MODE_ALLOWED
            
            // Log result
            try {
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().apply {
                    setCustomKey("usage_stats_permission", hasPermission)
                    setCustomKey("permission_check_method", "checkOpNoThrow")
                    log("UsageStats permission: $hasPermission")
                }
            } catch (e: Exception) {
                // Firebase not available, ignore
            }
            
            hasPermission
        } catch (e: Exception) {
            android.util.Log.e("PermissionHelper", "Error checking usage stats permission on ${Build.MANUFACTURER} ${Build.MODEL}", e)
            
            // Report to Crashlytics
            try {
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
            } catch (ignored: Exception) {
                // Firebase not available
            }
            
            false
        }
    }
    
    /**
     * Check if battery optimization is disabled
     */
    fun isBatteryOptimizationDisabled(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } else {
            true // Not applicable for older versions
        }
    }
    
    /**
     * Check if notification permission is granted (Android 13+)
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required for older versions
        }
    }
    
    /**
     * Open Accessibility Settings
     */
    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ContextCompat.startActivity(context, intent, null)
    }
    
    /**
     * Open Usage Stats Settings
     */
    fun openUsageStatsSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ContextCompat.startActivity(context, intent, null)
    }
    
    /**
     * Request battery optimization exemption
     */
    fun requestBatteryOptimizationExemption(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                ContextCompat.startActivity(context, intent, null)
            } catch (e: Exception) {
                // Fallback to battery settings
                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ContextCompat.startActivity(context, fallbackIntent, null)
            }
        }
    }
    
    /**
     * Open app notification settings
     */
    fun openNotificationSettings(context: Context) {
        val intent = Intent().apply {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            ContextCompat.startActivity(context, intent, null)
        } catch (e: Exception) {
            // Fallback to app settings
            val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ContextCompat.startActivity(context, fallbackIntent, null)
        }
    }
    
    /**
     * Check all required permissions
     */
    fun areAllPermissionsGranted(context: Context): Boolean {
        return isAccessibilityServiceEnabled(context) &&
                hasUsageStatsPermission(context)
    }
}
