package com.weyya.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weyya.app.data.db.dao.ScheduleDao
import com.weyya.app.data.db.dao.WhitelistDao
import com.weyya.app.data.db.entity.ScheduleEntity
import com.weyya.app.data.db.entity.WhitelistEntity
import com.weyya.app.data.prefs.UserPreferences
import com.weyya.app.data.telephony.SimInfo
import com.weyya.app.data.telephony.SimResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.weyya.app.data.prefs.DEFAULT_ATTEMPT_THRESHOLD
import com.weyya.app.data.prefs.DEFAULT_TIME_WINDOW_MINUTES
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferences,
    private val scheduleDao: ScheduleDao,
    private val whitelistDao: WhitelistDao,
    private val simResolver: SimResolver,
) : ViewModel() {

    private val _activeSims = MutableStateFlow<List<SimInfo>>(emptyList())
    val activeSims: StateFlow<List<SimInfo>> = _activeSims.asStateFlow()

    val hasDualSim: Boolean get() = simResolver.hasDualSim()

    fun hasPhonePermission(): Boolean = simResolver.hasPhonePermission()

    /** Call when the Settings screen opens or after the permission launcher returns. */
    fun refreshSims() {
        _activeSims.value = simResolver.getActiveSims()
    }

    val attemptThreshold: StateFlow<Int> = prefs.attemptThreshold
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_ATTEMPT_THRESHOLD)

    val timeWindowMinutes: StateFlow<Int> = prefs.timeWindowMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_TIME_WINDOW_MINUTES)

    val schedules: StateFlow<List<ScheduleEntity>> = scheduleDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setThreshold(value: Int) {
        viewModelScope.launch { prefs.setAttemptThreshold(value) }
    }

    fun setWindowMinutes(value: Int) {
        viewModelScope.launch { prefs.setTimeWindowMinutes(value) }
    }

    fun addSchedule(schedule: ScheduleEntity) {
        viewModelScope.launch { scheduleDao.insert(schedule) }
    }

    fun updateSchedule(schedule: ScheduleEntity) {
        viewModelScope.launch { scheduleDao.update(schedule) }
    }

    fun toggleSchedule(schedule: ScheduleEntity) {
        viewModelScope.launch { scheduleDao.update(schedule.copy(enabled = !schedule.enabled)) }
    }

    fun deleteSchedule(schedule: ScheduleEntity) {
        viewModelScope.launch { scheduleDao.delete(schedule) }
    }

    // Whitelist
    val whitelist: StateFlow<List<WhitelistEntity>> = whitelistDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addToWhitelist(phoneNumber: String, label: String = "") {
        viewModelScope.launch {
            whitelistDao.insert(WhitelistEntity(phoneNumber = phoneNumber, label = label))
        }
    }

    fun removeFromWhitelist(number: String) {
        viewModelScope.launch { whitelistDao.deleteByNumber(number) }
    }
}
