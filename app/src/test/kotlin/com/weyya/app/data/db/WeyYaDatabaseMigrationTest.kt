package com.weyya.app.data.db

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Validates the full Room migration chain (v1 → v5) against real SQLite via Robolectric,
 * so it runs in plain unit tests (`./gradlew test`) without an emulator.
 *
 * Strategy: build the database at an old version by hand, insert data, then open it through
 * Room with all migrations registered. Opening forces the migrations to run AND makes Room
 * validate that the resulting schema matches the current entities (identity hash) — a mismatch
 * throws. No historical schema JSONs are needed (only 5.json is exported).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WeyYaDatabaseMigrationTest {

    private val context: Context = RuntimeEnvironment.getApplication()
    private val dbName = "migration-test.db"

    // blocked_calls is unchanged across all versions (it existed in v1; no migration touches it).
    private val createBlockedCallsV1 =
        "CREATE TABLE IF NOT EXISTS `blocked_calls` (" +
            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`phoneNumber` TEXT, `timestamp` INTEGER NOT NULL, " +
            "`attemptCount` INTEGER NOT NULL, `wasEventuallyAllowed` INTEGER NOT NULL)"

    // schedules as introduced by MIGRATION_1_2 — single `dayOfWeek` INTEGER column.
    private val createSchedulesV2 =
        "CREATE TABLE IF NOT EXISTS `schedules` (" +
            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`dayOfWeek` INTEGER NOT NULL, `startTime` TEXT NOT NULL, " +
            "`endTime` TEXT NOT NULL, `enabled` INTEGER NOT NULL DEFAULT 1)"

    @After
    fun cleanup() {
        context.deleteDatabase(dbName)
    }

    /** Creates the DB at [version], runs [onCreated] against it, then closes the helper. */
    private fun createLegacyDb(version: Int, onCreated: (SupportSQLiteDatabase) -> Unit) {
        val callback = object : SupportSQLiteOpenHelper.Callback(version) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(createBlockedCallsV1)
                if (version >= 2) db.execSQL(createSchedulesV2)
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                // No-op: tests build directly at the version they need.
            }
        }
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(callback)
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        onCreated(helper.writableDatabase)
        helper.close()
    }

    private fun openWithRoom(): WeyYaDatabase =
        Room.databaseBuilder(context, WeyYaDatabase::class.java, dbName)
            .addMigrations(
                WeyYaDatabase.MIGRATION_1_2,
                WeyYaDatabase.MIGRATION_2_3,
                WeyYaDatabase.MIGRATION_3_4,
                WeyYaDatabase.MIGRATION_4_5,
            )
            .allowMainThreadQueries()
            .build()

    @Test
    fun migrate1To5_preservesBlockedCallAndValidatesSchema() = runBlocking {
        createLegacyDb(version = 1) { db ->
            db.execSQL(
                "INSERT INTO blocked_calls (phoneNumber, timestamp, attemptCount, wasEventuallyAllowed) " +
                    "VALUES ('5551234', 1000, 2, 0)",
            )
        }

        val room = openWithRoom()
        try {
            // Opening already ran v1→v5 and Room validated the final schema (would throw otherwise).
            val calls = room.blockedCallDao().getAll().first()
            assertThat(calls).hasSize(1)
            assertThat(calls[0].phoneNumber).isEqualTo("5551234")
            assertThat(calls[0].attemptCount).isEqualTo(2)
            // whitelist (added in v4) is queryable and empty.
            assertThat(room.scheduleDao().getAll().first()).isEmpty()
        } finally {
            room.close()
        }
    }

    @Test
    fun migrate2To5_convertsDayOfWeekIntToCsvAndAddsNullSimSlot() = runBlocking {
        createLegacyDb(version = 2) { db ->
            db.execSQL(
                "INSERT INTO schedules (dayOfWeek, startTime, endTime, enabled) " +
                    "VALUES (3, '22:00', '07:00', 1)",
            )
        }

        val room = openWithRoom()
        try {
            val schedules = room.scheduleDao().getAll().first()
            assertThat(schedules).hasSize(1)
            // MIGRATION_2_3 casts the INTEGER day into the comma-separated `daysOfWeek` string.
            assertThat(schedules[0].daysOfWeek).isEqualTo("3")
            assertThat(schedules[0].startTime).isEqualTo("22:00")
            assertThat(schedules[0].enabled).isTrue()
            // MIGRATION_4_5 adds simSlot defaulting to null (applies to every SIM).
            assertThat(schedules[0].simSlot).isNull()
        } finally {
            room.close()
        }
    }
}
