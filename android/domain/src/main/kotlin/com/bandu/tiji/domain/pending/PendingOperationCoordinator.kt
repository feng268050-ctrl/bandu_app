package com.bandu.tiji.domain.pending

class PendingOperationCoordinator {
    private var pending: PendingAiOperation? = null
    private var resumed: Boolean = false

    fun save(operation: PendingAiOperation) {
        pending = operation
        resumed = false
    }

    fun peek(): PendingAiOperation? = pending

    fun consumeForResume(): PendingAiOperation? {
        val operation = pending ?: return null
        if (resumed) return null
        resumed = true
        return operation
    }

    fun onConfigurationValidated(handler: (PendingAiOperation) -> Unit) {
        consumeForResume()?.let(handler)
    }

    fun clear() {
        pending = null
        resumed = false
    }
}
