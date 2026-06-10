package com.weyya.app.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Calendar

class TimeUtilsTest {

    @Test
    fun `todayStartMillis has zero hour minute second`() {
        val start = TimeUtils.todayStartMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = start }
        assertThat(cal.get(Calendar.HOUR_OF_DAY)).isEqualTo(0)
        assertThat(cal.get(Calendar.MINUTE)).isEqualTo(0)
        assertThat(cal.get(Calendar.SECOND)).isEqualTo(0)
        assertThat(cal.get(Calendar.MILLISECOND)).isEqualTo(0)
    }

    @Test
    fun `monthStartMillis is day 1`() {
        val start = TimeUtils.monthStartMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = start }
        assertThat(cal.get(Calendar.DAY_OF_MONTH)).isEqualTo(1)
        assertThat(cal.get(Calendar.HOUR_OF_DAY)).isEqualTo(0)
        assertThat(cal.get(Calendar.MINUTE)).isEqualTo(0)
    }

    @Test
    fun `todayStartMillis is not in the future`() {
        assertThat(TimeUtils.todayStartMillis()).isAtMost(System.currentTimeMillis())
    }

    @Test
    fun `monthStartMillis is before or equal to todayStartMillis`() {
        assertThat(TimeUtils.monthStartMillis()).isAtMost(TimeUtils.todayStartMillis())
    }

    @Test
    fun `daysAgoStartMillis one day is midnight of yesterday`() {
        assertIsMidnightDaysAgo(TimeUtils.daysAgoStartMillis(1), days = 1)
    }

    @Test
    fun `daysAgoStartMillis seven days is midnight one week ago`() {
        assertIsMidnightDaysAgo(TimeUtils.daysAgoStartMillis(7), days = 7)
    }

    // DST-safe: assert midnight wall-clock + correct calendar day rather than a fixed
    // millis offset, which would drift by an hour across a DST transition.
    private fun assertIsMidnightDaysAgo(actual: Long, days: Int) {
        val cal = Calendar.getInstance().apply { timeInMillis = actual }
        assertThat(cal.get(Calendar.HOUR_OF_DAY)).isEqualTo(0)
        assertThat(cal.get(Calendar.MINUTE)).isEqualTo(0)
        assertThat(cal.get(Calendar.SECOND)).isEqualTo(0)
        assertThat(cal.get(Calendar.MILLISECOND)).isEqualTo(0)

        val expected = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -days) }
        assertThat(cal.get(Calendar.YEAR)).isEqualTo(expected.get(Calendar.YEAR))
        assertThat(cal.get(Calendar.DAY_OF_YEAR)).isEqualTo(expected.get(Calendar.DAY_OF_YEAR))
    }

    @Test
    fun `daysBetween counts whole elapsed days`() {
        assertThat(TimeUtils.daysBetween(0L, 0L)).isEqualTo(0)
        assertThat(TimeUtils.daysBetween(0L, MILLIS_PER_DAY - 1)).isEqualTo(0)
        assertThat(TimeUtils.daysBetween(0L, MILLIS_PER_DAY)).isEqualTo(1)
        assertThat(TimeUtils.daysBetween(0L, 2 * MILLIS_PER_DAY + 1)).isEqualTo(2)
    }

    @Test
    fun `daysBetween floors negative spans to zero`() {
        assertThat(TimeUtils.daysBetween(MILLIS_PER_DAY, 0L)).isEqualTo(0)
    }

    private companion object {
        const val MILLIS_PER_DAY = 86_400_000L
    }
}
