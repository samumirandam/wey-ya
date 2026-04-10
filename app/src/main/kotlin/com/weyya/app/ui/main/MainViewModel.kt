package com.weyya.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weyya.app.data.db.dao.BlockedCallDao
import com.weyya.app.data.db.dao.ScheduleDao
import com.weyya.app.data.db.entity.BlockedCallEntity
import com.weyya.app.data.prefs.UserPreferences
import com.weyya.app.domain.ScheduleChecker
import com.weyya.app.domain.model.BlockingMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val prefs: UserPreferences,
    private val blockedCallDao: BlockedCallDao,
    private val scheduleDao: ScheduleDao,
    private val scheduleChecker: ScheduleChecker,
) : ViewModel() {

    val isActive: StateFlow<Boolean> = prefs.isActive
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val blockingMode: StateFlow<BlockingMode> = prefs.blockingMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BlockingMode.UNKNOWN_CALLERS)

    val blockedToday: StateFlow<Int> = blockedCallDao.getBlockedCountSince(todayStartMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val totalBlocked: StateFlow<Int> = blockedCallDao.getTotalBlockedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val lastBlocked: StateFlow<BlockedCallEntity?> = blockedCallDao.getLastBlocked()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val minuteTicker = flow {
        while (true) {
            emit(Unit)
            delay(60_000)
        }
    }

    val isWithinSchedule: StateFlow<Boolean> = combine(
        scheduleDao.getEnabled(),
        minuteTicker,
    ) { schedules, _ ->
        scheduleChecker.isBlockingActive(schedules)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _hasScreeningRole = MutableStateFlow(false)
    val hasScreeningRole: StateFlow<Boolean> = _hasScreeningRole.asStateFlow()

    fun setHasScreeningRole(hasRole: Boolean) {
        _hasScreeningRole.value = hasRole
    }

    fun toggle() {
        viewModelScope.launch {
            prefs.setActive(!isActive.value)
        }
    }

    fun setBlockingMode(mode: BlockingMode) {
        viewModelScope.launch {
            prefs.setBlockingMode(mode)
        }
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
