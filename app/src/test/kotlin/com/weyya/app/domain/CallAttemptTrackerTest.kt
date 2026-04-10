package com.weyya.app.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class CallAttemptTrackerTest {

    private lateinit var tracker: CallAttemptTracker

    @Before
    fun setup() {
        tracker = CallAttemptTracker()
    }

    @Test
    fun `first call returns count 1`() {
        val count = tracker.recordAndCount("+5215512345678", 5)
        assertThat(count).isEqualTo(1)
    }

    @Test
    fun `multiple calls increment count`() {
        val phone = "+5215512345678"
        tracker.recordAndCount(phone, 5)
        tracker.recordAndCount(phone, 5)
        val count = tracker.recordAndCount(phone, 5)
        assertThat(count).isEqualTo(3)
    }

    @Test
    fun `different numbers have independent counts`() {
        tracker.recordAndCount("+5215511111111", 5)
        tracker.recordAndCount("+5215511111111", 5)
        val count = tracker.recordAndCount("+5215522222222", 5)
        assertThat(count).isEqualTo(1)
    }

    @Test
    fun `getCount returns current count without recording`() {
        val phone = "+5215512345678"
        tracker.recordAndCount(phone, 5)
        tracker.recordAndCount(phone, 5)

        val count = tracker.getCount(phone, 5)
        assertThat(count).isEqualTo(2)
    }

    @Test
    fun `getCount returns 0 for unknown number`() {
        val count = tracker.getCount("+5215500000000", 5)
        assertThat(count).isEqualTo(0)
    }

    @Test
    fun `reset clears all entries`() {
        tracker.recordAndCount("+5215511111111", 5)
        tracker.recordAndCount("+5215522222222", 5)
        tracker.reset()

        assertThat(tracker.getCount("+5215511111111", 5)).isEqualTo(0)
        assertThat(tracker.getCount("+5215522222222", 5)).isEqualTo(0)
    }

    @Test
    fun `calls within same instant are both counted`() {
        val phone = "+5215512345678"
        tracker.recordAndCount(phone, 1)
        val count = tracker.recordAndCount(phone, 1)
        assertThat(count).isEqualTo(2)
    }

    @Test
    fun `old attempts expire after window`() {
        val phone = "+5215512345678"
        var now = 100_000L
        tracker.timeProvider = { now }

        tracker.recordAndCount(phone, 5)
        tracker.recordAndCount(phone, 5)
        assertThat(tracker.getCount(phone, 5)).isEqualTo(2)

        // Advance past the 5-minute window
        now += 5 * 60_000L + 1
        assertThat(tracker.getCount(phone, 5)).isEqualTo(0)
    }

    @Test
    fun `recordAndCount purges expired before counting`() {
        val phone = "+5215512345678"
        var now = 100_000L
        tracker.timeProvider = { now }

        tracker.recordAndCount(phone, 5)

        // Advance past window, then record again
        now += 5 * 60_000L + 1
        val count = tracker.recordAndCount(phone, 5)
        assertThat(count).isEqualTo(1) // old one expired, only new one counts
    }

    @Test
    fun `window zero discards all previous attempts`() {
        val phone = "+5215512345678"
        var now = 100_000L
        tracker.timeProvider = { now }

        tracker.recordAndCount(phone, 0)
        now += 1 // advance 1ms
        val count = tracker.recordAndCount(phone, 0)
        assertThat(count).isEqualTo(1) // cutoff = now, previous attempt is < now
    }

    @Test
    fun `cleanup triggers when exceeding MAX_TRACKED_NUMBERS`() {
        var now = 100_000L
        tracker.timeProvider = { now }

        // Record 101 unique numbers
        repeat(101) { i ->
            tracker.recordAndCount("+52155${i.toString().padStart(7, '0')}", 5)
        }

        // Advance past window so all attempts expire
        now += 5 * 60_000L + 1

        // Record one more to trigger cleanup
        tracker.recordAndCount("+5215599999999", 5)

        // Expired numbers should have been cleaned up
        assertThat(tracker.getCount("+52155${0.toString().padStart(7, '0')}", 5)).isEqualTo(0)
    }
}
