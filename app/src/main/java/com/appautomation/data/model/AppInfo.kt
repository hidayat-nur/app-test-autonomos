package com.appautomation.data.model

import android.graphics.drawable.Drawable

/**
 * Data class representing an installed application
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    val isSystemApp: Boolean = false
)
