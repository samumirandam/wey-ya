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
     * - No enabled schedules at all → true (block 24/7 when toggle is on)
     * - Enabled schedules exist but none apply to this SIM → false (this SIM is unrestricted)
     * - At least one applicable schedule → true only if current time falls within at least one
     *
     * callSimSlot null means the resolver couldn't identify the SIM (mono-SIM, permission
     * denied, hidden handle). In that case every enabled schedule applies — conservative
     * fallback that preserves pre-dual-SIM behavior.
     */
    fun isBlockingActive(
        schedules: List<ScheduleEntity>,
        callSimSlot: Int? = null,
        now: LocalDateTime = LocalDateTime.now(),
    ): Boolean {
        val allEnabled = schedules.filter { it.enabled }
        if (allEnabled.isEmpty()) return true

        val enabled = allEnabled.filter { schedule ->
            callSimSlot == null || schedule.simSlot == null || schedule.simSlot == callSimSlot
        }
        // Other SIMs have schedules but this call's SIM has none → unrestricted for this SIM
        if (enabled.isEmpty()) return false

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
