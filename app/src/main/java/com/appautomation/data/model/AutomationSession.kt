package com.appautomation.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for persisting automation sessions
 */
@Entity(tableName = "automation_sessions")
data class AutomationSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val appQueueJson: String, // JSON serialized list of AppTask
    val currentIndex: Int,
    val isActive: Boolean,
    val completedCount: Int = 0
)
