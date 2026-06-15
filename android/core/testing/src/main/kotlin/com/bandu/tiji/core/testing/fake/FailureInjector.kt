package com.bandu.tiji.core.testing.fake

class FailureInjector {
    private val queuedFailures = ArrayDeque<Throwable>()

    fun enqueue(throwable: Throwable) {
        queuedFailures.addLast(throwable)
    }

    fun clear() {
        queuedFailures.clear()
    }

    internal fun throwIfQueued() {
        if (queuedFailures.isNotEmpty()) {
            throw queuedFailures.removeFirst()
        }
    }
}
