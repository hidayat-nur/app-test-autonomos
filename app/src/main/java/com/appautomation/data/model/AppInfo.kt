package com.appautomation.data.model

import android.graphics.drawable.Drawable

/**
 * Data class representing an installed application
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    val isSystemApp: Boolean = false,
    val installTime: Long = 0L // Timestamp when app was installed
)

/**
 * Sorting options for app list
 */
enum class AppSortOption {
    NAME_ASC,           // A-Z
    NAME_DESC,          // Z-A
    INSTALL_NEWEST,     // Newest first
    INSTALL_OLDEST;     // Oldest first
    
    val displayName: String
        get() = when (this) {
            NAME_ASC -> "Name (A-Z)"
            NAME_DESC -> "Name (Z-A)"
            INSTALL_NEWEST -> "Newest installed"
            INSTALL_OLDEST -> "Oldest installed"
        }
}
