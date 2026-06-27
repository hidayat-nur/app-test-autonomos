package com.appautomation.util

/**
 * Computes notification progress as a percentage (0..100).
 *
 * Guards against division-by-zero: when [durationMillis] is zero or negative
 * (e.g. a resumed task whose remaining time was shortened), returns 0 instead
 * of throwing ArithmeticException.
 */
fun computeProgress(durationMillis: Long, remainingMillis: Long): Int {
    if (durationMillis <= 0) return 0
    val elapsed = durationMillis - remainingMillis
    val percent = (elapsed * 100 / durationMillis).toInt()
    return percent.coerceIn(0, 100)
}
