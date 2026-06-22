package com.bandu.tiji.domain.ai

class AiDataConsentCoordinator(
    initiallyAccepted: Boolean = false,
) {
    private var accepted = initiallyAccepted

    fun isAccepted(): Boolean = accepted

    fun accept() {
        accepted = true
    }

    fun reset() {
        accepted = false
    }
}
