package com.bandu.tiji.core.common.time

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ClockTest {
    @Test
    fun systemClock_returnsPositiveEpochMillis() {
        val before = System.currentTimeMillis()
        val actual = SystemClock().nowEpochMillis()
        val after = System.currentTimeMillis()

        assertThat(actual).isAtLeast(before)
        assertThat(actual).isAtMost(after)
    }
}

class FakeClock(private var fixedMillis: Long) : Clock {
    override fun nowEpochMillis(): Long = fixedMillis

    fun advance(millis: Long) {
        fixedMillis += millis
    }
}
