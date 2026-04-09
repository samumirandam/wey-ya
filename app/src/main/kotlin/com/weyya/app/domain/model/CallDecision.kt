package com.weyya.app.domain.model

sealed interface CallDecision {
    data object Allow : CallDecision
    data class Reject(val reason: String) : CallDecision
}
