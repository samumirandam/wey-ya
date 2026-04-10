package com.weyya.app.domain.model

sealed interface CallDecision {
    data class Allow(val reason: String = "") : CallDecision
    data class Reject(val reason: String) : CallDecision
}
