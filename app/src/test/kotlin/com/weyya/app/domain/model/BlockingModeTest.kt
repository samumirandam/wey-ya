package com.weyya.app.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BlockingModeTest {

    @Test
    fun `round-trips through storage string`() {
        for (mode in BlockingMode.entries) {
            assertThat(BlockingMode.fromString(mode.toStorageString())).isEqualTo(mode)
        }
    }

    @Test
    fun `unknown storage value falls back to UNKNOWN_CALLERS`() {
        assertThat(BlockingMode.fromString("garbage")).isEqualTo(BlockingMode.UNKNOWN_CALLERS)
    }

    @Test
    fun `storage strings are the expected stable keys`() {
        assertThat(BlockingMode.UNKNOWN_CALLERS.toStorageString()).isEqualTo("unknown")
        assertThat(BlockingMode.ALL_CALLERS.toStorageString()).isEqualTo("all")
    }
}
