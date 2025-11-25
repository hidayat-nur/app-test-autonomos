package com.appautomation.data.model

/**
 * Data class representing an app task in the automation queue
 */
data class AppTask(
    val packageName: String,
    val appName: String,
    val durationMillis: Long
) {
    companion object {
        const val DEFAULT_DURATION_MILLIS = 7 * 60 * 1000L // 7 minutes
    }
}
