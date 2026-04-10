package com.weyya.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayOfWeek: Int, // 1=Monday … 7=Sunday (ISO-8601)
    val startTime: String, // "HH:mm"
    val endTime: String, // "HH:mm"
    val enabled: Boolean = true,
)
