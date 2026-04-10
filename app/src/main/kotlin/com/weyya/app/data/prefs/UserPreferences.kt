package com.weyya.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.weyya.app.domain.model.BlockingMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

const val DEFAULT_ATTEMPT_THRESHOLD = 3
const val DEFAULT_TIME_WINDOW_MINUTES = 5

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

    val isActive: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.IS_ACTIVE] ?: false }

    val blockingMode: Flow<BlockingMode> = context.dataStore.data
        .map { BlockingMode.fromString(it[Keys.BLOCKING_MODE] ?: "unknown") }

    val attemptThreshold: Flow<Int> = context.dataStore.data
        .map { it[Keys.ATTEMPT_THRESHOLD] ?: DEFAULT_ATTEMPT_THRESHOLD }

    val timeWindowMinutes: Flow<Int> = context.dataStore.data
        .map { it[Keys.TIME_WINDOW_MINUTES] ?: DEFAULT_TIME_WINDOW_MINUTES }

    val firstActivationDate: Flow<Long?> = context.dataStore.data
        .map { it[Keys.FIRST_ACTIVATION_DATE] }

    val batteryDismissed: Flow<Boolean> = context.dataStore.data
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
        context.dataStore.edit { it[Keys.ATTEMPT_THRESHOLD] = threshold.coerceIn(1, 10) }
    }

    suspend fun setTimeWindowMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.TIME_WINDOW_MINUTES] = minutes.coerceIn(1, 30) }
    }

    suspend fun setBatteryDismissed(dismissed: Boolean) {
        context.dataStore.edit { it[Keys.BATTERY_DISMISSED] = dismissed }
    }
}
