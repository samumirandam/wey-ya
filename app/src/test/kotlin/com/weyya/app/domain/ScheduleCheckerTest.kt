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
            now = LocalDateTime.of(2026, 4, 9, 14, 0), // Thursday
        )
        assertThat(result).isTrue()
    }

    @Test
    fun `within normal schedule returns true`() {
        val wednesday = scheduleFor(dayOfWeek = 4, start = "09:00", end = "17:00")
        val result = checker.isBlockingActive(
            listOf(wednesday),
            now = LocalDateTime.of(2026, 4, 9, 12, 0), // Thursday 12:00
        )
        assertThat(result).isTrue()
    }

    @Test
    fun `outside normal schedule returns false`() {
        val wednesday = scheduleFor(dayOfWeek = 4, start = "09:00", end = "17:00")
        val result = checker.isBlockingActive(
            listOf(wednesday),
            now = LocalDateTime.of(2026, 4, 9, 20, 0), // Thursday 20:00
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `wrong day returns false`() {
        val monday = scheduleFor(dayOfWeek = 1, start = "09:00", end = "17:00")
        val result = checker.isBlockingActive(
            listOf(monday),
            now = LocalDateTime.of(2026, 4, 9, 12, 0), // Thursday
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `midnight-crossing schedule before midnight`() {
        val wednesday = scheduleFor(dayOfWeek = 4, start = "22:00", end = "07:00")
        val result = checker.isBlockingActive(
            listOf(wednesday),
            now = LocalDateTime.of(2026, 4, 9, 23, 0), // Thursday 23:00
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
            now = LocalDateTime.of(2026, 4, 10, 2, 0), // Friday 02:00
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
            now = LocalDateTime.of(2026, 4, 7, 3, 0), // Tuesday April 7 2026 03:00
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
            now = LocalDateTime.of(2026, 4, 8, 3, 0), // Wednesday April 8 03:00
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `disabled schedule is ignored`() {
        val wednesday = scheduleFor(dayOfWeek = 4, start = "09:00", end = "17:00", enabled = false)
        val result = checker.isBlockingActive(
            listOf(wednesday),
            now = LocalDateTime.of(2026, 4, 9, 12, 0),
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
            now = LocalDateTime.of(2026, 4, 9, 19, 0), // Thursday 19:00
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
            now = LocalDateTime.of(2026, 4, 9, 12, 0),
        )).isTrue()
        // Saturday 12:00 (day 6, NOT included)
        assertThat(checker.isBlockingActive(
            listOf(weekdays),
            now = LocalDateTime.of(2026, 4, 11, 12, 0),
        )).isFalse()
    }

    @Test
    fun `between multiple schedules returns false`() {
        val morning = scheduleFor(dayOfWeek = 4, start = "08:00", end = "12:00")
        val evening = scheduleFor(dayOfWeek = 4, start = "18:00", end = "22:00")
        val result = checker.isBlockingActive(
            listOf(morning, evening),
            now = LocalDateTime.of(2026, 4, 9, 15, 0), // Thursday 15:00
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `malformed time string returns false without crashing`() {
        val badSchedule = scheduleFor(dayOfWeek = 4, start = "invalid", end = "also-bad")
        val result = checker.isBlockingActive(
            listOf(badSchedule),
            now = LocalDateTime.of(2026, 4, 9, 12, 0), // Thursday
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
            now = LocalDateTime.of(2026, 4, 9, 12, 0),
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
            now = LocalDateTime.of(2026, 4, 6, 3, 0), // Monday April 6 2026 03:00
        )
        assertThat(result).isTrue()
    }

    @Test
    fun `midnight-crossing schedule active at exactly 00 00`() {
        val thursday = scheduleFor(dayOfWeek = 4, start = "22:00", end = "06:00")
        val result = checker.isBlockingActive(
            listOf(thursday),
            now = LocalDateTime.of(2026, 4, 10, 0, 0, 0), // Friday 00:00:00 (yesterday was Thursday)
        )
        assertThat(result).isTrue()
    }

    @Test
    fun `schedule for specific SIM applies only to that slot`() {
        val sim0Schedule = scheduleFor(dayOfWeek = 4, start = "09:00", end = "17:00", simSlot = 0)
        val thursdayNoon = LocalDateTime.of(2026, 4, 9, 12, 0)

        assertThat(checker.isBlockingActive(listOf(sim0Schedule), callSimSlot = 0, now = thursdayNoon))
            .isTrue()
        // Other SIM has schedules defined but this call is on a different slot → unrestricted
        assertThat(checker.isBlockingActive(listOf(sim0Schedule), callSimSlot = 1, now = thursdayNoon))
            .isFalse()
    }

    @Test
    fun `schedule with null simSlot applies to any SIM`() {
        val bothSims = scheduleFor(dayOfWeek = 4, start = "09:00", end = "17:00", simSlot = null)
        val thursdayNoon = LocalDateTime.of(2026, 4, 9, 12, 0)

        assertThat(checker.isBlockingActive(listOf(bothSims), callSimSlot = 0, now = thursdayNoon)).isTrue()
        assertThat(checker.isBlockingActive(listOf(bothSims), callSimSlot = 1, now = thursdayNoon)).isTrue()
    }

    @Test
    fun `unresolved callSimSlot falls back to evaluating every schedule`() {
        // When the resolver returns null, we conservatively evaluate every schedule
        // regardless of its simSlot — preserves pre-dual-SIM behavior.
        val sim0Schedule = scheduleFor(dayOfWeek = 4, start = "09:00", end = "17:00", simSlot = 0)
        val result = checker.isBlockingActive(
            listOf(sim0Schedule),
            callSimSlot = null,
            now = LocalDateTime.of(2026, 4, 9, 12, 0),
        )
        assertThat(result).isTrue()
    }

    @Test
    fun `no schedules at all still means block 24 by 7`() {
        // No schedules in DB, dual-SIM or not → always active
        assertThat(checker.isBlockingActive(emptyList(), callSimSlot = 0)).isTrue()
        assertThat(checker.isBlockingActive(emptyList(), callSimSlot = 1)).isTrue()
    }

    @Test
    fun `midnight-crossing SIM-specific schedule does not leak to other SIM`() {
        // 22:00-07:00 restricted to SIM 0. A 03:00 AM call on SIM 1 must pass through
        // (other SIM has schedules defined but this one is unrestricted).
        val sim0Night = scheduleFor(dayOfWeek = 1, start = "22:00", end = "07:00", simSlot = 0)
        val result = checker.isBlockingActive(
            listOf(sim0Night),
            callSimSlot = 1,
            now = LocalDateTime.of(2026, 4, 7, 3, 0), // Tuesday 03:00 (yesterday was Monday)
        )
        assertThat(result).isFalse()
    }

    @Test
    fun `disabled SIM-specific schedule does not shadow enabled one for the same SIM`() {
        // One schedule is disabled for SIM 0, another enabled for SIM 1. A call on SIM 1 in
        // the enabled window must block — the disabled row must not bias the filter.
        val sim0Disabled = scheduleFor(dayOfWeek = 4, start = "09:00", end = "17:00", enabled = false, simSlot = 0)
        val sim1Enabled = scheduleFor(dayOfWeek = 4, start = "09:00", end = "17:00", enabled = true, simSlot = 1)
        val result = checker.isBlockingActive(
            listOf(sim0Disabled, sim1Enabled),
            callSimSlot = 1,
            now = LocalDateTime.of(2026, 4, 9, 12, 0), // Thursday noon
        )
        assertThat(result).isTrue()
    }

    @Test
    fun `global schedule applies to any specific callSimSlot`() {
        // Schedule is global (simSlot = null). Both dual-SIM calls should be affected exactly
        // like in the legacy mono-SIM path, just with an explicit slot.
        val globalSchedule = scheduleFor(dayOfWeek = 4, start = "09:00", end = "17:00", simSlot = null)
        val thursdayNoon = LocalDateTime.of(2026, 4, 9, 12, 0)

        assertThat(checker.isBlockingActive(listOf(globalSchedule), callSimSlot = 0, now = thursdayNoon))
            .isTrue()
        assertThat(checker.isBlockingActive(listOf(globalSchedule), callSimSlot = 1, now = thursdayNoon))
            .isTrue()
    }

    @Test
    fun `mixed schedules each SIM restricted only to its own window`() {
        // SIM 0 blocks 09-17; SIM 1 blocks 20-23. Verify calls are only restricted
        // by the schedule belonging to the incoming SIM.
        val sim0Day = scheduleFor(dayOfWeek = 4, start = "09:00", end = "17:00", simSlot = 0)
        val sim1Night = scheduleFor(dayOfWeek = 4, start = "20:00", end = "23:00", simSlot = 1)
        val schedules = listOf(sim0Day, sim1Night)

        val thursdayNoon = LocalDateTime.of(2026, 4, 9, 12, 0)
        val thursdayEvening = LocalDateTime.of(2026, 4, 9, 21, 0)

        // SIM 0 call at noon → within SIM 0 window → block
        assertThat(checker.isBlockingActive(schedules, callSimSlot = 0, now = thursdayNoon)).isTrue()
        // SIM 1 call at noon → SIM 1's window is 20-23, not now → unrestricted
        assertThat(checker.isBlockingActive(schedules, callSimSlot = 1, now = thursdayNoon)).isFalse()
        // SIM 1 call at 21:00 → inside SIM 1 window → block
        assertThat(checker.isBlockingActive(schedules, callSimSlot = 1, now = thursdayEvening)).isTrue()
        // SIM 0 call at 21:00 → SIM 0 window was 09-17, now outside → don't block
        assertThat(checker.isBlockingActive(schedules, callSimSlot = 0, now = thursdayEvening)).isFalse()
    }

    private fun scheduleFor(
        dayOfWeek: Int,
        start: String,
        end: String,
        enabled: Boolean = true,
        simSlot: Int? = null,
    ) = ScheduleEntity(
        id = 0,
        daysOfWeek = dayOfWeek.toString(),
        startTime = start,
        endTime = end,
        enabled = enabled,
        simSlot = simSlot,
    )
}
