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
}
