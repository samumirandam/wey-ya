package com.weyya.app.service

import android.telecom.Call
import android.telecom.CallScreeningService
import com.weyya.app.data.contacts.ContactsResolver
import com.weyya.app.data.db.dao.BlockedCallDao
import com.weyya.app.data.db.dao.ScheduleDao
import com.weyya.app.data.db.entity.BlockedCallEntity
import com.weyya.app.data.prefs.UserPreferences
import com.weyya.app.domain.CallDecisionEngine
import com.weyya.app.domain.ScheduleChecker
import com.weyya.app.domain.model.CallDecision
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class WeyYaScreeningService : CallScreeningService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ScreeningEntryPoint {
        fun callDecisionEngine(): CallDecisionEngine
        fun userPreferences(): UserPreferences
        fun contactsResolver(): ContactsResolver
        fun blockedCallDao(): BlockedCallDao
        fun scheduleDao(): ScheduleDao
        fun scheduleChecker(): ScheduleChecker
    }

    override fun onScreenCall(callDetails: Call.Details) {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            ScreeningEntryPoint::class.java,
        )

        val number = callDetails.handle?.schemeSpecificPart
        val prefs = entryPoint.userPreferences()

        val isActive = runBlocking { prefs.isActive.first() }
        val mode = runBlocking { prefs.blockingMode.first() }
        val threshold = runBlocking { prefs.attemptThreshold.first() }
        val window = runBlocking { prefs.timeWindowMinutes.first() }

        val isContact = number?.let { entryPoint.contactsResolver().isContact(it) } ?: false

        val schedules = runBlocking { entryPoint.scheduleDao().getEnabledSync() }
        val isWithinSchedule = entryPoint.scheduleChecker().isBlockingActive(schedules)

        val decision = entryPoint.callDecisionEngine().decide(
            isActive = isActive,
            mode = mode,
            phoneNumber = number,
            isContact = isContact,
            isWhitelisted = false,
            attemptThreshold = threshold,
            windowMinutes = window,
            isWithinSchedule = isWithinSchedule,
        )

        val response = when (decision) {
            is CallDecision.Allow -> {
                CallResponse.Builder()
                    .setDisallowCall(false)
                    .setRejectCall(false)
                    .setSkipCallLog(false)
                    .setSkipNotification(false)
                    .build()
            }
            is CallDecision.Reject -> {
                CoroutineScope(Dispatchers.IO).launch {
                    entryPoint.blockedCallDao().insert(
                        BlockedCallEntity(
                            phoneNumber = number,
                            timestamp = System.currentTimeMillis(),
                            attemptCount = 1,
                            wasEventuallyAllowed = false,
                        ),
                    )
                }
                CallResponse.Builder()
                    .setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipCallLog(false)
                    .setSkipNotification(true)
                    .build()
            }
        }

        respondToCall(callDetails, response)
    }
}
