package com.weyya.app.domain.model

enum class BlockingMode {
    UNKNOWN_CALLERS,
    ALL_CALLERS;

    companion object {
        fun fromString(value: String): BlockingMode =
            when (value) {
                "all" -> ALL_CALLERS
                else -> UNKNOWN_CALLERS
            }
    }

    fun toStorageString(): String =
        when (this) {
            UNKNOWN_CALLERS -> "unknown"
            ALL_CALLERS -> "all"
        }
}
