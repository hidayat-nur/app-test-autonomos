package com.appautomation

import android.app.Application
import android.os.Build
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AutomationApplication : Application() {
    
    companion object {
        private const val TAG = "AutomationApp"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // 🔥 Initialize Firebase Crashlytics
        initializeCrashlytics()
        
        // Log device info
        logDeviceInfo()
        
        Log.d(TAG, "✅ App initialized with remote crash monitoring")
    }
    
    private fun initializeCrashlytics() {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            
            // Enable crash collection
            crashlytics.setCrashlyticsCollectionEnabled(true)
            
            // Set device info as custom keys
            crashlytics.setCustomKey("manufacturer", Build.MANUFACTURER)
            crashlytics.setCustomKey("model", Build.MODEL)
            crashlytics.setCustomKey("android_version", Build.VERSION.RELEASE)
            crashlytics.setCustomKey("api_level", Build.VERSION.SDK_INT)
            crashlytics.setCustomKey("brand", Build.BRAND)
            crashlytics.setCustomKey("device", Build.DEVICE)
            
            // Log if device is problematic (Motorola E6, VIVO Y71, etc)
            val isProblematic = Build.MANUFACTURER.equals("motorola", ignoreCase = true) ||
                                Build.MANUFACTURER.equals("vivo", ignoreCase = true)
            crashlytics.setCustomKey("is_problematic_device", isProblematic)
            
            if (isProblematic) {
                crashlytics.log("⚠️ Problematic device detected: ${Build.MANUFACTURER} ${Build.MODEL} API ${Build.VERSION.SDK_INT}")
            }
            
            // Set user ID (optional - bisa pakai random ID)
            crashlytics.setUserId("${Build.MANUFACTURER}_${Build.MODEL}_${System.currentTimeMillis()}")
            
            Log.d(TAG, "🔥 Firebase Crashlytics initialized")
            Log.d(TAG, "📊 Dashboard: https://console.firebase.google.com/project/YOUR_PROJECT/crashlytics")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Crashlytics", e)
        }
    }
    
    private fun logDeviceInfo() {
        Log.d(TAG, "═══════════════════════════════════════")
        Log.d(TAG, "📱 APP STARTED - DEVICE INFO:")
        Log.d(TAG, "   Manufacturer: ${Build.MANUFACTURER}")
        Log.d(TAG, "   Model: ${Build.MODEL}")
        Log.d(TAG, "   Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        Log.d(TAG, "   Brand: ${Build.BRAND}")
        Log.d(TAG, "   Device: ${Build.DEVICE}")
        
        // Detect problematic devices
        val deviceStatus = when {
            Build.MANUFACTURER.equals("vivo", ignoreCase = true) -> "⚠️ VIVO - Known restrictions"
            Build.MANUFACTURER.equals("motorola", ignoreCase = true) && 
                Build.MODEL.contains("E6", ignoreCase = true) -> "⚠️ Motorola E6 - Budget device"
            Build.MANUFACTURER.equals("oppo", ignoreCase = true) -> "⚠️ OPPO - ColorOS restrictions"
            Build.MANUFACTURER.equals("xiaomi", ignoreCase = true) -> "⚠️ Xiaomi - MIUI restrictions"
            else -> "✅ Standard device"
        }
        Log.d(TAG, "   Status: $deviceStatus")
        Log.d(TAG, "═══════════════════════════════════════")
        
        // Log to Crashlytics untuk debug non-fatal
        try {
            FirebaseCrashlytics.getInstance().log(
                "App started on ${Build.MANUFACTURER} ${Build.MODEL} - Android ${Build.VERSION.RELEASE} - Status: $deviceStatus"
            )
        } catch (e: Exception) {
            // Silent fail
        }
    }
}
