package com.weyya.app.domain

import com.google.common.truth.Truth.assertThat
import com.weyya.app.data.db.entity.ScheduleEntity
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class ScheduleCheckerTest {

    private lateinit var checker: ScheduleChecker

    @Before
    fun setup() {
        checker = ScheduleChecker()
    }

    @Test
    fun `no schedules means always active`() {
        val result = checker.isBlockingActive(
            emptyList(),
            LocalDateTime.of(2026, 4, 9, 14, 0), // Thursday
        )
        assertThat(result).isTrue()
    }

    @Test
    fun `within normal schedule returns true`() {
        val wednesday = scheduleFor(dayOfWeek = 4, start = "09:00", end = "17:00")
        val result = checker.isBlockingActive(
            listOf(wednesday),
            LocalDateTime.of(2026, 4, 9, 12, 0), // Thursday 12:00
        )
        assertThat(result).isTrue()
    }

    @Test
    fun `outside normal schedule returns false`() {
        val wednesday = scheduleFor(dayOfWeek = 4, start = "09:00", end = "17:00")
        val result = checker.isBlockingActive(
            listOf(wednesday),
            LocalDateTime.of(2026, 4, 9, 20, 0), // Thursday 20:00
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `wrong day returns false`() {
        val monday = scheduleFor(dayOfWeek = 1, start = "09:00", end = "17:00")
        val result = checker.isBlockingActive(
            listOf(monday),
            LocalDateTime.of(2026, 4, 9, 12, 0), // Thursday
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `midnight-crossing schedule before midnight`() {
        val wednesday = scheduleFor(dayOfWeek = 4, start = "22:00", end = "07:00")
        val result = checker.isBlockingActive(
            listOf(wednesday),
            LocalDateTime.of(2026, 4, 9, 23, 0), // Thursday 23:00
        )
        assertThat(result).isTrue()
    }

    @Test
    fun `midnight-crossing schedule after midnight same day`() {
        // Schedule on Thursday(4) 22:00-07:00, means Thu 22:00 → Fri 07:00
        // At Friday 02:00, yesterday was Thursday (day=4) → in range
        val thursday = scheduleFor(dayOfWeek = 4, start = "22:00", end = "07:00")
        val result = checker.isBlockingActive(
            listOf(thursday),
            LocalDateTime.of(2026, 4, 10, 2, 0), // Friday 02:00
        )
        assertThat(result).isTrue()
    }

    @Test
    fun `midnight-crossing schedule active on next day AM`() {
        // Schedule on Monday(1) 22:00-07:00
        // At Tuesday 03:00, yesterday was Monday → should be active
        val monday = scheduleFor(dayOfWeek = 1, start = "22:00", end = "07:00")
        val result = checker.isBlockingActive(
            listOf(monday),
            LocalDateTime.of(2026, 4, 7, 3, 0), // Tuesday April 7 2026 03:00
        )
        assertThat(result).isTrue()
    }

    @Test
    fun `midnight-crossing schedule NOT active on wrong day AM`() {
        // Schedule on Monday(1) 22:00-07:00
        // At Wednesday 03:00, yesterday was Tuesday → NOT active
        val monday = scheduleFor(dayOfWeek = 1, start = "22:00", end = "07:00")
        val result = checker.isBlockingActive(
            listOf(monday),
            LocalDateTime.of(2026, 4, 8, 3, 0), // Wednesday April 8 03:00
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `disabled schedule is ignored`() {
        val wednesday = scheduleFor(dayOfWeek = 4, start = "09:00", end = "17:00", enabled = false)
        val result = checker.isBlockingActive(
            listOf(wednesday),
            LocalDateTime.of(2026, 4, 9, 12, 0),
        )
        // All enabled schedules are empty → always active
        assertThat(result).isTrue()
    }

    @Test
    fun `multiple schedules any match returns true`() {
        val morning = scheduleFor(dayOfWeek = 4, start = "08:00", end = "12:00")
        val evening = scheduleFor(dayOfWeek = 4, start = "18:00", end = "22:00")
        val result = checker.isBlockingActive(
            listOf(morning, evening),
            LocalDateTime.of(2026, 4, 9, 19, 0), // Thursday 19:00
        )
        assertThat(result).isTrue()
    }

    @Test
    fun `multi-day schedule matches any included day`() {
        val weekdays = ScheduleEntity(
            id = 0,
            daysOfWeek = "1,2,3,4,5",
            startTime = "09:00",
            endTime = "17:00",
            enabled = true,
        )
        // Thursday 12:00 (day 4, included in 1,2,3,4,5)
        assertThat(checker.isBlockingActive(
            listOf(weekdays),
            LocalDateTime.of(2026, 4, 9, 12, 0),
        )).isTrue()
        // Saturday 12:00 (day 6, NOT included)
        assertThat(checker.isBlockingActive(
            listOf(weekdays),
            LocalDateTime.of(2026, 4, 11, 12, 0),
        )).isFalse()
    }

    @Test
    fun `between multiple schedules returns false`() {
        val morning = scheduleFor(dayOfWeek = 4, start = "08:00", end = "12:00")
        val evening = scheduleFor(dayOfWeek = 4, start = "18:00", end = "22:00")
        val result = checker.isBlockingActive(
            listOf(morning, evening),
            LocalDateTime.of(2026, 4, 9, 15, 0), // Thursday 15:00
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `malformed time string returns false without crashing`() {
        val badSchedule = scheduleFor(dayOfWeek = 4, start = "invalid", end = "also-bad")
        val result = checker.isBlockingActive(
            listOf(badSchedule),
            LocalDateTime.of(2026, 4, 9, 12, 0), // Thursday
        )
        // With no valid enabled schedules matching, falls back to "always active"
        // but the malformed schedule itself returns false from the any{} lambda
        assertThat(result).isFalse()
    }

    @Test
    fun `startTime equals endTime crosses midnight path`() {
        val schedule = scheduleFor(dayOfWeek = 4, start = "09:00", end = "09:00")
        // At 09:00 the crossesMidnight logic applies: start >= end means crosses midnight
        // So the check is: now >= start || now < end → 12:00 >= 09:00 → true
        val result = checker.isBlockingActive(
            listOf(schedule),
            LocalDateTime.of(2026, 4, 9, 12, 0),
        )
        assertThat(result).isTrue()
    }

    @Test
    fun `Sunday to Monday midnight crossing`() {
        // Schedule on Sunday(7) 22:00-07:00
        // At Monday 03:00, yesterday was Sunday → should be active
        val sunday = scheduleFor(dayOfWeek = 7, start = "22:00", end = "07:00")
        val result = checker.isBlockingActive(
            listOf(sunday),
            LocalDateTime.of(2026, 4, 6, 3, 0), // Monday April 6 2026 03:00
        )
        assertThat(result).isTrue()
    }

    @Test
    fun `midnight-crossing schedule active at exactly 00 00`() {
        val thursday = scheduleFor(dayOfWeek = 4, start = "22:00", end = "06:00")
        val result = checker.isBlockingActive(
            listOf(thursday),
            LocalDateTime.of(2026, 4, 10, 0, 0, 0), // Friday 00:00:00 (yesterday was Thursday)
        )
        assertThat(result).isTrue()
    }

    private fun scheduleFor(
        dayOfWeek: Int,
        start: String,
        end: String,
        enabled: Boolean = true,
    ) = ScheduleEntity(
        id = 0,
        daysOfWeek = dayOfWeek.toString(),
        startTime = start,
        endTime = end,
        enabled = enabled,
    )
}
