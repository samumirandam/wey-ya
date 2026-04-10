package com.weyya.app.ui.privacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weyya.app.data.db.dao.BlockedCallDao
import com.weyya.app.data.prefs.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class PrivacyDashboardViewModel @Inject constructor(
    prefs: UserPreferences,
    blockedCallDao: BlockedCallDao,
) : ViewModel() {

    val totalBlocked: StateFlow<Int> = blockedCallDao.getTotalBlockedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val blockedThisMonth: StateFlow<Int> = blockedCallDao.getBlockedCountSince(monthStartMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val bypassCount: StateFlow<Int> = blockedCallDao.getBypassCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val daysSinceFirstActivation: StateFlow<Int> = prefs.firstActivationDate
        .map { firstDate ->
            if (firstDate == null) 0
            else ((System.currentTimeMillis() - firstDate) / 86_400_000).toInt().coerceAtLeast(0)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private fun monthStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
