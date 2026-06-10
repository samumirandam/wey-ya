package com.weyya.app.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CsvUtilsTest {

    @Test
    fun `plain value is returned unchanged`() {
        assertThat(CsvUtils.csvField("+5215512345678")).isEqualTo("+5215512345678")
    }

    @Test
    fun `value with comma is quoted`() {
        assertThat(CsvUtils.csvField("a,b")).isEqualTo("\"a,b\"")
    }

    @Test
    fun `value with double quote is quoted and escaped`() {
        // RFC 4180: embedded `"` is doubled, whole field wrapped.
        assertThat(CsvUtils.csvField("a\"b")).isEqualTo("\"a\"\"b\"")
    }

    @Test
    fun `value with newline is quoted`() {
        assertThat(CsvUtils.csvField("a\nb")).isEqualTo("\"a\nb\"")
    }

    @Test
    fun `value with carriage return is quoted`() {
        assertThat(CsvUtils.csvField("a\rb")).isEqualTo("\"a\rb\"")
    }
}
