package com.weyya.app.data.prefs

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.weyya.app.domain.model.BlockingMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

const val DEFAULT_ATTEMPT_THRESHOLD = 3
const val MIN_ATTEMPT_THRESHOLD = 2
const val MAX_ATTEMPT_THRESHOLD = 10
const val DEFAULT_TIME_WINDOW_MINUTES = 5
const val MIN_TIME_WINDOW_MINUTES = 1
const val MAX_TIME_WINDOW_MINUTES = 30

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "weyya_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private object Keys {
        val IS_ACTIVE = booleanPreferencesKey("is_active")
        val BLOCKING_MODE = stringPreferencesKey("blocking_mode")
        val ATTEMPT_THRESHOLD = intPreferencesKey("attempt_threshold")
        val TIME_WINDOW_MINUTES = intPreferencesKey("time_window_minutes")
        val FIRST_ACTIVATION_DATE = longPreferencesKey("first_activation_date")
        val BATTERY_DISMISSED = booleanPreferencesKey("battery_dismissed")
    }

    // A corrupt or unreadable DataStore throws IOException on read. Degrade to empty
    // preferences (defaults) instead of crashing the call-screening path.
    private val data: Flow<Preferences> = context.dataStore.data
        .catch { e ->
            if (e is IOException) {
                Log.e("UserPreferences", "Failed to read preferences, using defaults", e)
                emit(emptyPreferences())
            } else {
                throw e
            }
        }

    val isActive: Flow<Boolean> = data
        .map { it[Keys.IS_ACTIVE] ?: false }

    val blockingMode: Flow<BlockingMode> = data
        .map { BlockingMode.fromString(it[Keys.BLOCKING_MODE] ?: "unknown") }

    // Coerce on read as well as write: a value persisted below the floor by an older
    // build (e.g. threshold 1) is self-healed instead of reaching the decision engine.
    val attemptThreshold: Flow<Int> = data
        .map {
            (it[Keys.ATTEMPT_THRESHOLD] ?: DEFAULT_ATTEMPT_THRESHOLD)
                .coerceIn(MIN_ATTEMPT_THRESHOLD, MAX_ATTEMPT_THRESHOLD)
        }

    val timeWindowMinutes: Flow<Int> = data
        .map {
            (it[Keys.TIME_WINDOW_MINUTES] ?: DEFAULT_TIME_WINDOW_MINUTES)
                .coerceIn(MIN_TIME_WINDOW_MINUTES, MAX_TIME_WINDOW_MINUTES)
        }

    val firstActivationDate: Flow<Long?> = data
        .map { it[Keys.FIRST_ACTIVATION_DATE] }

    val batteryDismissed: Flow<Boolean> = data
        .map { it[Keys.BATTERY_DISMISSED] ?: false }

    suspend fun setActive(active: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_ACTIVE] = active
            if (active && prefs[Keys.FIRST_ACTIVATION_DATE] == null) {
                prefs[Keys.FIRST_ACTIVATION_DATE] = System.currentTimeMillis()
            }
        }
    }

    suspend fun setBlockingMode(mode: BlockingMode) {
        context.dataStore.edit { it[Keys.BLOCKING_MODE] = mode.toStorageString() }
    }

    suspend fun setAttemptThreshold(threshold: Int) {
        // Floor of MIN_ATTEMPT_THRESHOLD: a threshold of 1 lets every unknown caller through
        // on the first attempt, defeating the block. The Settings slider exposes 2..5; the
        // wider clamp is a safety bound for any value arriving from outside the UI.
        context.dataStore.edit {
            it[Keys.ATTEMPT_THRESHOLD] = threshold.coerceIn(MIN_ATTEMPT_THRESHOLD, MAX_ATTEMPT_THRESHOLD)
        }
    }

    suspend fun setTimeWindowMinutes(minutes: Int) {
        context.dataStore.edit {
            it[Keys.TIME_WINDOW_MINUTES] = minutes.coerceIn(MIN_TIME_WINDOW_MINUTES, MAX_TIME_WINDOW_MINUTES)
        }
    }

    suspend fun setBatteryDismissed(dismissed: Boolean) {
        context.dataStore.edit { it[Keys.BATTERY_DISMISSED] = dismissed }
    }
}
