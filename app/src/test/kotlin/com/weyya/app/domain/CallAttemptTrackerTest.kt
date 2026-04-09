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
    fun `small window keeps only recent attempts`() {
        val phone = "+5215512345678"
        // Both calls happen within the same millisecond window
        tracker.recordAndCount(phone, 1)
        val count = tracker.recordAndCount(phone, 1)
        assertThat(count).isEqualTo(2)
    }
}
