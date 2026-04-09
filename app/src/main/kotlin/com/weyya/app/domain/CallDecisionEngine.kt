package com.weyya.app.domain

import com.weyya.app.domain.model.BlockingMode
import com.weyya.app.domain.model.CallDecision
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallDecisionEngine @Inject constructor(
    private val attemptTracker: CallAttemptTracker,
) {

    fun decide(
        isActive: Boolean,
        mode: BlockingMode,
        phoneNumber: String?,
        isContact: Boolean,
        isWhitelisted: Boolean,
        attemptThreshold: Int,
        windowMinutes: Int,
    ): CallDecision {
        if (!isActive) return CallDecision.Allow

        if (mode == BlockingMode.UNKNOWN_CALLERS && (isContact || isWhitelisted)) {
            return CallDecision.Allow
        }

        if (phoneNumber == null) {
            return CallDecision.Reject("Hidden number")
        }

        val count = attemptTracker.recordAndCount(phoneNumber, windowMinutes)
        if (count >= attemptThreshold) {
            return CallDecision.Allow
        }

        return CallDecision.Reject("Attempt $count/$attemptThreshold")
    }
}
