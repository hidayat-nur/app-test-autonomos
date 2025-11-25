package com.appautomation.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for logging automation history
 */
@Entity(tableName = "automation_logs")
data class AutomationLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val appPackage: String,
    val appName: String,
    val durationMillis: Long,
    val success: Boolean,
    val errorMessage: String? = null
)
