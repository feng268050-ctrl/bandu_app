package com.bandu.tiji.core.testing.time

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TestClockTest {
    @Test
    fun `clock can be set and advanced deterministically`() {
        val clock = TestClock(initialEpochMillis = 1_000L)

        assertThat(clock.nowEpochMillis()).isEqualTo(1_000L)
        clock.advanceBy(250L)
        assertThat(clock.nowEpochMillis()).isEqualTo(1_250L)
        clock.setEpochMillis(5_000L)
        assertThat(clock.nowEpochMillis()).isEqualTo(5_000L)
    }

    @Test
    fun `clock rejects negative advancement`() {
        val clock = TestClock()

        val error = runCatching { clock.advanceBy(-1L) }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(clock.nowEpochMillis()).isEqualTo(0L)
    }
}
