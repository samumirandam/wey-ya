package com.weyya.app.widget

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.weyya.app.data.prefs.UserPreferences
import com.weyya.app.domain.model.BlockingMode
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.util.Calendar

data class WidgetState(
    val isActive: Boolean,
    val modeName: String,
    val blockedToday: Int,
)

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun userPreferences(): UserPreferences
    fun blockedCallDao(): com.weyya.app.data.db.dao.BlockedCallDao
}

object WidgetDataHelper {

    private fun entryPoint(context: Context): WidgetEntryPoint =
        EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)

    suspend fun readState(context: Context): WidgetState {
        val ep = entryPoint(context)
        val prefs = ep.userPreferences()
        val isActive = prefs.isActive.first()
        val mode = prefs.blockingMode.first()
        val blockedToday = ep.blockedCallDao().getBlockedCountSince(todayStartMillis()).first()

        val modeName = when (mode) {
            BlockingMode.UNKNOWN_CALLERS -> "Desconocidos"
            BlockingMode.ALL_CALLERS -> "Bloquear todo"
        }

        return WidgetState(isActive, modeName, blockedToday)
    }

    suspend fun toggle(context: Context) {
        val prefs = entryPoint(context).userPreferences()
        val current = prefs.isActive.first()
        prefs.setActive(!current)
    }

    private fun todayStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
