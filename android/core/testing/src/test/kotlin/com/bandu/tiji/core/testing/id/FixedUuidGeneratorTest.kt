package com.bandu.tiji.core.testing.id

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FixedUuidGeneratorTest {
    @Test
    fun `returns configured value for every request`() {
        val generator = FixedUuidGenerator("fixed-id")

        assertThat(generator.newUuid()).isEqualTo("fixed-id")
        assertThat(generator.newUuid()).isEqualTo("fixed-id")
    }

    @Test
    fun `rejects blank value`() {
        val error = runCatching { FixedUuidGenerator(" ") }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
    }
}
