package com.appautomation.service

import com.appautomation.data.model.AppTask
import org.junit.Assert.assertEquals
import org.junit.Test

class ResumePlanTest {

    private fun app(name: String, duration: Long = 60_000L) =
        AppTask(packageName = "pkg.$name", appName = name, durationMillis = duration)

    @Test
    fun `resume preserves the full remaining queue`() {
        val current = app("B", duration = 60_000L)
        val queue = listOf(app("C"), app("D"), app("E"))
        val paused = PausedSession(
            currentApp = current,
            remainingTimeMillis = 20_000L,
            remainingQueue = queue,
            completedCount = 1,
            totalCount = 5,
            elapsedTimeMillis = 70_000L
        )

        val plan = buildResumePlan(paused, nowMillis = 1_000_000L)

        // current (shortened) + the 3 queued apps = 4 apps, NOT just 1
        assertEquals(4, plan.apps.size)
        assertEquals(listOf("B", "C", "D", "E"), plan.apps.map { it.appName })
    }

    @Test
    fun `resumed current app uses remaining duration`() {
        val paused = PausedSession(
            currentApp = app("B", duration = 60_000L),
            remainingTimeMillis = 20_000L,
            remainingQueue = emptyList(),
            completedCount = 0,
            totalCount = 1,
            elapsedTimeMillis = 40_000L
        )

        val plan = buildResumePlan(paused, nowMillis = 1_000_000L)

        assertEquals(20_000L, plan.apps.first().durationMillis)
    }

    @Test
    fun `queued apps keep their full duration`() {
        val queue = listOf(app("C", duration = 90_000L))
        val paused = PausedSession(
            currentApp = app("B"),
            remainingTimeMillis = 10_000L,
            remainingQueue = queue,
            completedCount = 0,
            totalCount = 2,
            elapsedTimeMillis = 0L
        )

        val plan = buildResumePlan(paused, nowMillis = 0L)

        assertEquals(90_000L, plan.apps[1].durationMillis)
    }

    @Test
    fun `completed and total counts are preserved`() {
        val paused = PausedSession(
            currentApp = app("C"),
            remainingTimeMillis = 5_000L,
            remainingQueue = listOf(app("D")),
            completedCount = 2,
            totalCount = 4,
            elapsedTimeMillis = 123_000L
        )

        val plan = buildResumePlan(paused, nowMillis = 500_000L)

        assertEquals(2, plan.startCompletedCount)
        assertEquals(4, plan.totalCount)
    }

    @Test
    fun `session start time is shifted so elapsed stays continuous`() {
        val paused = PausedSession(
            currentApp = app("A"),
            remainingTimeMillis = 5_000L,
            remainingQueue = emptyList(),
            completedCount = 0,
            totalCount = 1,
            elapsedTimeMillis = 30_000L
        )

        val plan = buildResumePlan(paused, nowMillis = 1_000_000L)

        // elapsed at resume == now - sessionStartTime == 30_000
        assertEquals(30_000L, 1_000_000L - plan.sessionStartTime)
    }

    @Test
    fun `negative remaining time is clamped to zero`() {
        val paused = PausedSession(
            currentApp = app("A", duration = 10_000L),
            remainingTimeMillis = -500L,
            remainingQueue = emptyList(),
            completedCount = 0,
            totalCount = 1,
            elapsedTimeMillis = 0L
        )

        val plan = buildResumePlan(paused, nowMillis = 0L)

        assertEquals(0L, plan.apps.first().durationMillis)
    }
}
