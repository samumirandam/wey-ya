package com.weyya.app.domain

import com.weyya.app.data.db.entity.ScheduleEntity
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleChecker @Inject constructor() {

    /**
     * Returns true if blocking should be active based on schedules.
     * - No enabled schedules → true (block 24/7 when toggle is on)
     * - Has enabled schedules → true only if current time falls within at least one
     */
    fun isBlockingActive(schedules: List<ScheduleEntity>, now: LocalDateTime = LocalDateTime.now()): Boolean {
        val enabled = schedules.filter { it.enabled }
        if (enabled.isEmpty()) return true

        val todayIso = now.dayOfWeek.value // 1=Monday … 7=Sunday
        val currentTime = now.toLocalTime()

        return enabled.any { schedule ->
            schedule.dayOfWeek == todayIso && isTimeInRange(currentTime, schedule.startTime, schedule.endTime)
        }
    }

    private fun isTimeInRange(current: LocalTime, start: String, end: String): Boolean {
        val startTime = LocalTime.parse(start)
        val endTime = LocalTime.parse(end)

        return if (endTime > startTime) {
            // Normal range: e.g. 09:00-17:00
            current in startTime..endTime
        } else {
            // Crosses midnight: e.g. 22:00-07:00
            current >= startTime || current <= endTime
        }
    }
}
