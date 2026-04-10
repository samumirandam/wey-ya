package com.weyya.app.service

import android.telecom.Call
import android.telecom.CallScreeningService
import com.weyya.app.data.contacts.ContactsResolver
import com.weyya.app.data.db.dao.BlockedCallDao
import com.weyya.app.data.db.dao.ScheduleDao
import com.weyya.app.data.db.dao.WhitelistDao
import com.weyya.app.data.db.entity.BlockedCallEntity
import com.weyya.app.data.prefs.UserPreferences
import com.weyya.app.domain.CallAttemptTracker
import com.weyya.app.domain.CallDecisionEngine
import com.weyya.app.domain.ScheduleChecker
import com.weyya.app.domain.model.CallDecision
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class WeyYaScreeningService : CallScreeningService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ScreeningEntryPoint {
        fun callDecisionEngine(): CallDecisionEngine
        fun callAttemptTracker(): CallAttemptTracker
        fun userPreferences(): UserPreferences
        fun contactsResolver(): ContactsResolver
        fun blockedCallDao(): BlockedCallDao
        fun scheduleDao(): ScheduleDao
        fun scheduleChecker(): ScheduleChecker
        fun whitelistDao(): WhitelistDao
    }

    override fun onScreenCall(callDetails: Call.Details) {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            ScreeningEntryPoint::class.java,
        )

        val number = callDetails.handle?.schemeSpecificPart

        val (isActive, mode, threshold, window, isContact, isWhitelisted, isWithinSchedule) =
            runBlocking(Dispatchers.IO) {
                val prefs = entryPoint.userPreferences()
                ScreeningParams(
                    isActive = prefs.isActive.first(),
                    mode = prefs.blockingMode.first(),
                    threshold = prefs.attemptThreshold.first(),
                    window = prefs.timeWindowMinutes.first(),
                    isContact = number?.let { entryPoint.contactsResolver().isContact(it) } ?: false,
                    isWhitelisted = number?.let { entryPoint.whitelistDao().isWhitelisted(it) } ?: false,
                    isWithinSchedule = entryPoint.scheduleChecker().isBlockingActive(
                        entryPoint.scheduleDao().getEnabledSync(),
                    ),
                )
            }

        val decision = entryPoint.callDecisionEngine().decide(
            isActive = isActive,
            mode = mode,
            phoneNumber = number,
            isContact = isContact,
            isWhitelisted = isWhitelisted,
            attemptThreshold = threshold,
            windowMinutes = window,
            isWithinSchedule = isWithinSchedule,
        )

        val tracker = entryPoint.callAttemptTracker()
        val attemptCount = number?.let { tracker.getCount(it, window) } ?: 0

        val response = when (decision) {
            is CallDecision.Allow -> {
                if (decision.reason == "bypass" && number != null) {
                    runBlocking(Dispatchers.IO) {
                        entryPoint.blockedCallDao().insert(
                            BlockedCallEntity(
                                phoneNumber = number,
                                timestamp = System.currentTimeMillis(),
                                attemptCount = attemptCount,
                                wasEventuallyAllowed = true,
                            ),
                        )
                    }
                }
                CallResponse.Builder()
                    .setDisallowCall(false)
                    .setRejectCall(false)
                    .setSkipCallLog(false)
                    .setSkipNotification(false)
                    .build()
            }
            is CallDecision.Reject -> {
                runBlocking(Dispatchers.IO) {
                    entryPoint.blockedCallDao().insert(
                        BlockedCallEntity(
                            phoneNumber = number,
                            timestamp = System.currentTimeMillis(),
                            attemptCount = attemptCount,
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

    private data class ScreeningParams(
        val isActive: Boolean,
        val mode: com.weyya.app.domain.model.BlockingMode,
        val threshold: Int,
        val window: Int,
        val isContact: Boolean,
        val isWhitelisted: Boolean,
        val isWithinSchedule: Boolean,
    )
}
