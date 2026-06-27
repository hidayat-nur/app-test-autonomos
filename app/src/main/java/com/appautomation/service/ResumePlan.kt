package com.appautomation.service

import com.appautomation.data.model.AppTask

/**
 * Snapshot captured when automation is paused. Holds everything needed to
 * resume the run without losing the queue (B1).
 */
data class PausedSession(
    val currentApp: AppTask,
    val remainingTimeMillis: Long,
    val remainingQueue: List<AppTask>,
    val completedCount: Int,
    val totalCount: Int,
    val elapsedTimeMillis: Long
)

/**
 * Plan describing how to resume a paused automation run.
 */
data class ResumePlan(
    val apps: List<AppTask>,
    val startCompletedCount: Int,
    val totalCount: Int,
    val sessionStartTime: Long
)

/**
 * Builds a resume plan that preserves the full remaining queue.
 *
 * The paused app is resumed with its shortened remaining duration, followed by
 * the rest of the queue at full duration. Completed/total counts are preserved,
 * and elapsed time is frozen across the pause (sessionStartTime is shifted so
 * that elapsed continues from where it left off rather than counting the pause).
 */
fun buildResumePlan(paused: PausedSession, nowMillis: Long): ResumePlan {
    val resumedCurrent = paused.currentApp.copy(
        durationMillis = paused.remainingTimeMillis.coerceAtLeast(0)
    )
    return ResumePlan(
        apps = listOf(resumedCurrent) + paused.remainingQueue,
        startCompletedCount = paused.completedCount,
        totalCount = paused.totalCount,
        sessionStartTime = nowMillis - paused.elapsedTimeMillis
    )
}
