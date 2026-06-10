package com.weyya.app.domain.model

/**
 * Pure-domain representation of a blocking schedule. Decoupled from Room's
 * ScheduleEntity so the domain layer stays free of Android/data dependencies.
 * Days are pre-parsed ISO day numbers (1=Monday … 7=Sunday).
 */
data class Schedule(
    val days: Set<Int>,
    val startTime: String, // "HH:mm"
    val endTime: String, // "HH:mm"
    val enabled: Boolean = true,
    val simSlot: Int? = null, // null = applies to every SIM; 0/1 = specific slot
)
