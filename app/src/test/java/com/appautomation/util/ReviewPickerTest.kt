package com.appautomation.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ReviewPickerTest {

    private val templates = listOf("A", "B", "C", "D")

    @Test
    fun `returns a template from the pool`() {
        val pick = ReviewPicker.pick(templates, exclude = null, random = Random(1))
        assertTrue(pick in templates)
    }

    @Test
    fun `never returns the excluded template when alternatives exist`() {
        // Run many times with different seeds; must never equal the excluded one.
        repeat(50) { seed ->
            val pick = ReviewPicker.pick(templates, exclude = "B", random = Random(seed.toLong()))
            assertNotEquals("B", pick)
        }
    }

    @Test
    fun `single template is returned even if excluded`() {
        val pick = ReviewPicker.pick(listOf("only"), exclude = "only", random = Random(0))
        assertEquals("only", pick)
    }

    @Test
    fun `empty templates returns empty string`() {
        assertEquals("", ReviewPicker.pick(emptyList(), exclude = null, random = Random(0)))
    }

    @Test
    fun `deterministic for a fixed seed`() {
        val a = ReviewPicker.pick(templates, exclude = null, random = Random(42))
        val b = ReviewPicker.pick(templates, exclude = null, random = Random(42))
        assertEquals(a, b)
    }
}
