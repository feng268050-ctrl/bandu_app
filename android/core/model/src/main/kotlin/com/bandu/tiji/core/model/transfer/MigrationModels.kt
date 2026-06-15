package com.bandu.tiji.core.model.transfer

data class TransferProgress(
    val phase: TransferPhase,
    val percentComplete: Int,
    val transferredBytes: Long,
    val totalBytes: Long,
    val bytesPerSecond: Long,
)

enum class TransferPhase {
    PAIRING,
    OFFER,
    TRANSFERRING,
    VERIFYING,
    COMMITTING,
    COMPLETED,
    FAILED,
}
