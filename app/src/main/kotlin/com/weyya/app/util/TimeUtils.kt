package com.weyya.app.util

import java.util.Calendar

object TimeUtils {

    private const val MILLIS_PER_DAY = 86_400_000L

    /**
     * Whole days elapsed between two instants, truncated and floored at 0.
     * A negative span (now before first) yields 0.
     */
    fun daysBetween(firstMillis: Long, nowMillis: Long): Int =
        ((nowMillis - firstMillis) / MILLIS_PER_DAY).toInt().coerceAtLeast(0)

    fun todayStartMillis(): Long = daysAgoStartMillis(0)

    fun daysAgoStartMillis(days: Int): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -days)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun monthStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
