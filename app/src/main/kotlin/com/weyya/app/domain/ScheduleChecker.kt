package com.weyya.app.domain

import android.util.Log
import com.weyya.app.data.db.entity.ScheduleEntity
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeParseException
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

        val yesterdayIso = now.minusDays(1).dayOfWeek.value

        return enabled.any { schedule ->
            val startTime: LocalTime
            val endTime: LocalTime
            try {
                startTime = LocalTime.parse(schedule.startTime)
                endTime = LocalTime.parse(schedule.endTime)
            } catch (_: DateTimeParseException) {
                Log.w("ScheduleChecker", "Malformed schedule time: ${schedule.startTime}-${schedule.endTime}")
                return@any false
            }
            val crossesMidnight = endTime <= startTime

            if (crossesMidnight) {
                // PM part: today's day + time >= start. AM part: yesterday's day + time <= end.
                (todayIso in schedule.daysList() && currentTime >= startTime) ||
                    (yesterdayIso in schedule.daysList() && currentTime <= endTime)
            } else {
                todayIso in schedule.daysList() && currentTime in startTime..endTime
            }
        }
    }
}
