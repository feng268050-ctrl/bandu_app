package com.bandu.tiji.core.testing.time

import com.bandu.tiji.core.common.time.Clock

class TestClock(
    initialEpochMillis: Long = 0L,
) : Clock {
    var currentEpochMillis: Long = initialEpochMillis
        private set

    override fun nowEpochMillis(): Long = currentEpochMillis

    fun setEpochMillis(epochMillis: Long) {
        currentEpochMillis = epochMillis
    }

    fun advanceBy(millis: Long) {
        require(millis >= 0L) { "millis must not be negative" }
        currentEpochMillis = Math.addExact(currentEpochMillis, millis)
    }
}
