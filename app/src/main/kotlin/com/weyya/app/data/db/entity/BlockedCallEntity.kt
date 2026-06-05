package com.weyya.app.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Indexed on timestamp: the log query orders by it and the stats queries filter by it (timestamp >= since).
@Entity(tableName = "blocked_calls", indices = [Index("timestamp")])
data class BlockedCallEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String?,
    val timestamp: Long,
    val attemptCount: Int,
    val wasEventuallyAllowed: Boolean = false,
)
