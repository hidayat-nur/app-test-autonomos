package com.appautomation.util

import kotlin.random.Random

/**
 * Picks a review text at random from a pool, avoiding the one used last so
 * consecutive ratings don't repeat the same text.
 */
object ReviewPicker {

    fun pick(templates: List<String>, exclude: String?, random: Random = Random.Default): String {
        if (templates.isEmpty()) return ""
        val pool = templates.filter { it != exclude }.ifEmpty { templates }
        return pool[random.nextInt(pool.size)]
    }
}
