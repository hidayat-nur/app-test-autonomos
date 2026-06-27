package com.appautomation.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressCalculatorTest {

    @Test
    fun `zero duration returns zero and does not throw`() {
        assertEquals(0, computeProgress(durationMillis = 0, remainingMillis = 0))
    }

    @Test
    fun `negative duration returns zero`() {
        assertEquals(0, computeProgress(durationMillis = -100, remainingMillis = 50))
    }

    @Test
    fun `half elapsed returns fifty percent`() {
        assertEquals(50, computeProgress(durationMillis = 1000, remainingMillis = 500))
    }

    @Test
    fun `no time elapsed returns zero`() {
        assertEquals(0, computeProgress(durationMillis = 1000, remainingMillis = 1000))
    }

    @Test
    fun `fully elapsed returns one hundred`() {
        assertEquals(100, computeProgress(durationMillis = 1000, remainingMillis = 0))
    }

    @Test
    fun `remaining greater than duration is clamped to zero`() {
        assertEquals(0, computeProgress(durationMillis = 1000, remainingMillis = 1500))
    }

    @Test
    fun `negative remaining is clamped to one hundred`() {
        assertEquals(100, computeProgress(durationMillis = 1000, remainingMillis = -200))
    }
}
