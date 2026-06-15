package com.bandu.tiji.domain.transfer

import com.bandu.tiji.core.model.transfer.TransferProgress

sealed interface TransferState {
    data object Idle : TransferState

    data object Discovering : TransferState

    data class Pairing(
        val peer: NearbyDevice,
        val expiresAt: Long,
    ) : TransferState

    data class AwaitingOfferConfirmation(
        val offer: TransferOfferSummary,
    ) : TransferState

    data class Transferring(
        val progress: TransferProgress,
    ) : TransferState

    data class Verifying(
        val progress: Int,
    ) : TransferState

    data object AwaitingFinalConfirmation : TransferState

    data object Committing : TransferState

    data class Completed(
        val summary: TransferSummary,
    ) : TransferState

    data class Failed(
        val code: TransferFailureCode,
        val resumable: Boolean,
    ) : TransferState
}
