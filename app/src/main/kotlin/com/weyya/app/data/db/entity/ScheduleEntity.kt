package com.weyya.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val daysOfWeek: String, // Comma-separated ISO days: "1,2,3,4,5" (1=Monday … 7=Sunday)
    val startTime: String, // "HH:mm"
    val endTime: String, // "HH:mm"
    val enabled: Boolean = true,
) {
    fun daysList(): List<Int> = daysOfWeek.split(",").map { it.trim().toInt() }

    companion object {
        fun daysToString(days: Collection<Int>): String = days.sorted().joinToString(",")
    }
}
