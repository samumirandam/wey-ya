package com.weyya.app.domain

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallAttemptTracker @Inject constructor() {

    private val attempts = ConcurrentHashMap<String, MutableList<Long>>()
    internal var timeProvider: () -> Long = System::currentTimeMillis

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
        const val MAX_TRACKED_NUMBERS = 100
    }

    fun recordAndCount(phoneNumber: String, windowMinutes: Int): Int {
        val now = timeProvider()
        val cutoff = now - (windowMinutes * MILLIS_PER_MINUTE)
        val list = attempts.computeIfAbsent(phoneNumber) { mutableListOf() }
        val count: Int
        synchronized(list) {
            list.removeAll { it < cutoff }
            list.add(now)
            count = list.size
        }
        if (attempts.size > MAX_TRACKED_NUMBERS) {
            attempts.entries.removeIf { (_, v) -> synchronized(v) { v.isEmpty() } }
        }
        return count
    }

    fun getCount(phoneNumber: String, windowMinutes: Int): Int {
        val cutoff = timeProvider() - (windowMinutes * MILLIS_PER_MINUTE)
        val list = attempts[phoneNumber] ?: return 0
        synchronized(list) {
            list.removeAll { it < cutoff }
            if (list.isEmpty()) {
                attempts.remove(phoneNumber)
                return 0
            }
            return list.size
        }
    }

    fun reset() {
        attempts.clear()
    }
}
