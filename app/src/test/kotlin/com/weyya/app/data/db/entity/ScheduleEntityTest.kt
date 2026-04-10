package com.weyya.app.data.db.entity

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ScheduleEntityTest {

    private fun entity(daysOfWeek: String) = ScheduleEntity(
        id = 0,
        daysOfWeek = daysOfWeek,
        startTime = "09:00",
        endTime = "17:00",
    )

    @Test
    fun `empty string returns empty list`() {
        assertThat(entity("").daysList()).isEmpty()
    }

    @Test
    fun `non-numeric values filtered out`() {
        assertThat(entity("abc").daysList()).isEmpty()
    }

    @Test
    fun `out of range values filtered`() {
        assertThat(entity("0,8").daysList()).isEmpty()
    }

    @Test
    fun `mixed valid and invalid`() {
        assertThat(entity("1,3,abc,7").daysList()).containsExactly(1, 3, 7)
    }

    @Test
    fun `valid days parsed correctly`() {
        assertThat(entity("1,2,3,4,5").daysList()).containsExactly(1, 2, 3, 4, 5)
    }

    @Test
    fun `whitespace around values is trimmed`() {
        assertThat(entity(" 1 , 3 , 5 ").daysList()).containsExactly(1, 3, 5)
    }

    @Test
    fun `daysToString sorts and joins`() {
        assertThat(ScheduleEntity.daysToString(listOf(5, 1, 3))).isEqualTo("1,3,5")
    }
}
