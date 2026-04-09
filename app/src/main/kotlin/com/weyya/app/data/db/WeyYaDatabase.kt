package com.weyya.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.weyya.app.data.db.dao.BlockedCallDao
import com.weyya.app.data.db.entity.BlockedCallEntity

@Database(
    entities = [BlockedCallEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class WeyYaDatabase : RoomDatabase() {
    abstract fun blockedCallDao(): BlockedCallDao
}
