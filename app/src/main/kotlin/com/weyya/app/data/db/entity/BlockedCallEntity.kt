package com.weyya.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_calls")
data class BlockedCallEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String?,
    val timestamp: Long,
    val attemptCount: Int,
    val wasEventuallyAllowed: Boolean = false,
)
