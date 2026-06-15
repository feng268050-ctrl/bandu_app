package com.bandu.tiji.core.common.time

interface Clock {
    fun nowEpochMillis(): Long
}

class SystemClock : Clock {
    override fun nowEpochMillis(): Long = System.currentTimeMillis()
}
