package com.weyya.app.domain

import com.google.common.truth.Truth.assertThat
import com.weyya.app.domain.model.BlockingMode
import com.weyya.app.domain.model.CallDecision
import org.junit.Before
import org.junit.Test

class CallDecisionEngineTest {

    private lateinit var tracker: CallAttemptTracker
    private lateinit var engine: CallDecisionEngine

    @Before
    fun setup() {
        tracker = CallAttemptTracker()
        engine = CallDecisionEngine(tracker)
    }

    @Test
    fun `when not active, always allows`() {
        val result = engine.decide(
            isActive = false,
            mode = BlockingMode.UNKNOWN_CALLERS,
            phoneNumber = "+5215512345678",
            isContact = false,
            isWhitelisted = false,
            attemptThreshold = 3,
            windowMinutes = 5,
        )
        assertThat(result).isEqualTo(CallDecision.Allow)
    }

    @Test
    fun `unknown mode allows contacts`() {
        val result = engine.decide(
            isActive = true,
            mode = BlockingMode.UNKNOWN_CALLERS,
            phoneNumber = "+5215512345678",
            isContact = true,
            isWhitelisted = false,
            attemptThreshold = 3,
            windowMinutes = 5,
        )
        assertThat(result).isEqualTo(CallDecision.Allow)
    }

    @Test
    fun `unknown mode allows whitelisted`() {
        val result = engine.decide(
            isActive = true,
            mode = BlockingMode.UNKNOWN_CALLERS,
            phoneNumber = "+5215512345678",
            isContact = false,
            isWhitelisted = true,
            attemptThreshold = 3,
            windowMinutes = 5,
        )
        assertThat(result).isEqualTo(CallDecision.Allow)
    }

    @Test
    fun `unknown mode rejects unknown caller on first attempt`() {
        val result = engine.decide(
            isActive = true,
            mode = BlockingMode.UNKNOWN_CALLERS,
            phoneNumber = "+5215512345678",
            isContact = false,
            isWhitelisted = false,
            attemptThreshold = 3,
            windowMinutes = 5,
        )
        assertThat(result).isInstanceOf(CallDecision.Reject::class.java)
    }

    @Test
    fun `all mode rejects contacts`() {
        val result = engine.decide(
            isActive = true,
            mode = BlockingMode.ALL_CALLERS,
            phoneNumber = "+5215512345678",
            isContact = true,
            isWhitelisted = false,
            attemptThreshold = 3,
            windowMinutes = 5,
        )
        assertThat(result).isInstanceOf(CallDecision.Reject::class.java)
    }

    @Test
    fun `all mode rejects whitelisted`() {
        val result = engine.decide(
            isActive = true,
            mode = BlockingMode.ALL_CALLERS,
            phoneNumber = "+5215512345678",
            isContact = false,
            isWhitelisted = true,
            attemptThreshold = 3,
            windowMinutes = 5,
        )
        assertThat(result).isInstanceOf(CallDecision.Reject::class.java)
    }

    @Test
    fun `rejects null phone number (hidden)`() {
        val result = engine.decide(
            isActive = true,
            mode = BlockingMode.UNKNOWN_CALLERS,
            phoneNumber = null,
            isContact = false,
            isWhitelisted = false,
            attemptThreshold = 3,
            windowMinutes = 5,
        )
        assertThat(result).isEqualTo(CallDecision.Reject("Hidden number"))
    }

    @Test
    fun `persistence bypass allows after threshold attempts`() {
        val phone = "+5215512345678"
        val threshold = 3

        // First two attempts should be rejected
        repeat(threshold - 1) {
            val result = engine.decide(
                isActive = true,
                mode = BlockingMode.UNKNOWN_CALLERS,
                phoneNumber = phone,
                isContact = false,
                isWhitelisted = false,
                attemptThreshold = threshold,
                windowMinutes = 5,
            )
            assertThat(result).isInstanceOf(CallDecision.Reject::class.java)
        }

        // Third attempt should be allowed (bypass)
        val result = engine.decide(
            isActive = true,
            mode = BlockingMode.UNKNOWN_CALLERS,
            phoneNumber = phone,
            isContact = false,
            isWhitelisted = false,
            attemptThreshold = threshold,
            windowMinutes = 5,
        )
        assertThat(result).isEqualTo(CallDecision.Allow)
    }

    @Test
    fun `persistence bypass works in all mode too`() {
        val phone = "+5215500000000"
        val threshold = 2

        // First attempt rejected
        engine.decide(
            isActive = true,
            mode = BlockingMode.ALL_CALLERS,
            phoneNumber = phone,
            isContact = true,
            isWhitelisted = true,
            attemptThreshold = threshold,
            windowMinutes = 5,
        )

        // Second attempt should bypass
        val result = engine.decide(
            isActive = true,
            mode = BlockingMode.ALL_CALLERS,
            phoneNumber = phone,
            isContact = true,
            isWhitelisted = true,
            attemptThreshold = threshold,
            windowMinutes = 5,
        )
        assertThat(result).isEqualTo(CallDecision.Allow)
    }

    @Test
    fun `different phone numbers have independent counters`() {
        val phone1 = "+5215511111111"
        val phone2 = "+5215522222222"

        engine.decide(
            isActive = true,
            mode = BlockingMode.UNKNOWN_CALLERS,
            phoneNumber = phone1,
            isContact = false,
            isWhitelisted = false,
            attemptThreshold = 3,
            windowMinutes = 5,
        )

        val result = engine.decide(
            isActive = true,
            mode = BlockingMode.UNKNOWN_CALLERS,
            phoneNumber = phone2,
            isContact = false,
            isWhitelisted = false,
            attemptThreshold = 3,
            windowMinutes = 5,
        )

        assertThat(result).isEqualTo(CallDecision.Reject("Attempt 1/3"))
    }
}
