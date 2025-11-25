package com.appautomation.service

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.util.Log
import com.appautomation.data.model.AppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLauncher @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "AppLauncher"
    }
    
    /**
     * Launch an app by package name
     */
    fun launchApp(packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                context.startActivity(intent)
                Log.d(TAG, "Successfully launched: $packageName")
                true
            } else {
                Log.e(TAG, "No launch intent found for: $packageName")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch $packageName", e)
            false
        }
    }
    
    /**
     * Get list of all launchable installed apps
     */
    fun getInstalledApps(includeSystemApps: Boolean = false): List<AppInfo> {
        return try {
            val packageManager = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            
            packageManager.queryIntentActivities(intent, 0)
                .mapNotNull { resolveInfo ->
                    try {
                        val packageName = resolveInfo.activityInfo.packageName
                        val appInfo = packageManager.getApplicationInfo(packageName, 0)
                        val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        
                        if (!includeSystemApps && isSystemApp) {
                            return@mapNotNull null
                        }
                        
                        AppInfo(
                            packageName = packageName,
                            appName = resolveInfo.loadLabel(packageManager).toString(),
                            icon = resolveInfo.loadIcon(packageManager),
                            isSystemApp = isSystemApp
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading app info", e)
                        null
                    }
                }
                .distinctBy { it.packageName }
                .sortedBy { it.appName.lowercase() }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting installed apps", e)
            emptyList()
        }
    }
    
    /**
     * Check if an app is installed
     */
    fun isAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Go to home screen
     */
    fun goToHome() {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to go home", e)
        }
    }
}
