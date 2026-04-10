package com.weyya.app.domain

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallAttemptTracker @Inject constructor() {

    private val attempts = ConcurrentHashMap<String, MutableList<Long>>()

    fun recordAndCount(phoneNumber: String, windowMinutes: Int): Int {
        val now = System.currentTimeMillis()
        val cutoff = now - (windowMinutes * 60_000L)
        val list = attempts.computeIfAbsent(phoneNumber) { mutableListOf() }
        synchronized(list) {
            list.removeAll { it < cutoff }
            list.add(now)
            return list.size
        }
    }

    fun getCount(phoneNumber: String, windowMinutes: Int): Int {
        val cutoff = System.currentTimeMillis() - (windowMinutes * 60_000L)
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
