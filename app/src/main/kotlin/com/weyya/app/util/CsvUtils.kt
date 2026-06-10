package com.weyya.app.util

object CsvUtils {

    private val NEEDS_QUOTING = Regex("[,\"\n\r]")

    /**
     * Escapes a single CSV field per RFC 4180: wraps in double quotes and doubles any
     * embedded quote when the value contains a comma, quote, or line break.
     */
    fun csvField(value: String): String =
        if (value.contains(NEEDS_QUOTING)) "\"${value.replace("\"", "\"\"")}\"" else value
}
