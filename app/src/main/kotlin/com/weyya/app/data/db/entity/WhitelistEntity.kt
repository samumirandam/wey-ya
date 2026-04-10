package com.weyya.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "whitelist")
data class WhitelistEntity(
    @PrimaryKey val phoneNumber: String,
    val label: String = "",
    val addedAt: Long = System.currentTimeMillis(),
)
