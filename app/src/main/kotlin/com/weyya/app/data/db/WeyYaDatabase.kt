package com.weyya.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.weyya.app.data.db.dao.BlockedCallDao
import com.weyya.app.data.db.dao.ScheduleDao
import com.weyya.app.data.db.entity.BlockedCallEntity
import com.weyya.app.data.db.entity.ScheduleEntity

@Database(
    entities = [BlockedCallEntity::class, ScheduleEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class WeyYaDatabase : RoomDatabase() {
    abstract fun blockedCallDao(): BlockedCallDao
    abstract fun scheduleDao(): ScheduleDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `schedules` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `dayOfWeek` INTEGER NOT NULL,
                        `startTime` TEXT NOT NULL,
                        `endTime` TEXT NOT NULL,
                        `enabled` INTEGER NOT NULL DEFAULT 1
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `schedules_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `daysOfWeek` TEXT NOT NULL,
                        `startTime` TEXT NOT NULL,
                        `endTime` TEXT NOT NULL,
                        `enabled` INTEGER NOT NULL DEFAULT 1
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `schedules_new` (`id`, `daysOfWeek`, `startTime`, `endTime`, `enabled`)
                    SELECT `id`, CAST(`dayOfWeek` AS TEXT), `startTime`, `endTime`, `enabled`
                    FROM `schedules`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `schedules`")
                db.execSQL("ALTER TABLE `schedules_new` RENAME TO `schedules`")
            }
        }
    }
}
