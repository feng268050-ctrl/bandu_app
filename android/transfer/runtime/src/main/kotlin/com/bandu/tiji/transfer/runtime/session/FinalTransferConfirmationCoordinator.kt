package com.bandu.tiji.transfer.runtime.session

import com.bandu.tiji.transfer.runtime.commit.CommitResult
import com.bandu.tiji.transfer.runtime.commit.TransferCommitCoordinator

class FinalTransferConfirmationCoordinator(
    private val commitCoordinator: TransferCommitCoordinator,
) {
    private val sessions = mutableMapOf<String, FinalConfirmationState>()

    suspend fun localConfirm(
        sessionId: String,
        inactiveSlot: String,
    ): FinalConfirmationResult =
        update(sessionId) { it.copy(localConfirmed = true) }
            .commitIfReady(sessionId, inactiveSlot)

    suspend fun peerConfirm(
        sessionId: String,
        inactiveSlot: String,
    ): FinalConfirmationResult =
        update(sessionId) { it.copy(peerConfirmed = true) }
            .commitIfReady(sessionId, inactiveSlot)

    fun cancel(sessionId: String): FinalConfirmationResult {
        sessions.remove(sessionId)
        return FinalConfirmationResult.Cancelled
    }

    fun state(sessionId: String): FinalConfirmationState =
        sessions[sessionId] ?: FinalConfirmationState()

    private fun update(
        sessionId: String,
        transform: (FinalConfirmationState) -> FinalConfirmationState,
    ): FinalConfirmationState {
        require(sessionId.isNotBlank()) { "Session ID must not be blank" }
        val next = transform(state(sessionId))
        sessions[sessionId] = next
        return next
    }

    private suspend fun FinalConfirmationState.commitIfReady(
        sessionId: String,
        inactiveSlot: String,
    ): FinalConfirmationResult =
        if (localConfirmed && peerConfirmed) {
            sessions.remove(sessionId)
            when (val result = commitCoordinator.commitVerifiedImport(sessionId, inactiveSlot)) {
                is CommitResult.Committed -> FinalConfirmationResult.Committed(result.activeSlot)
                is CommitResult.RolledBack -> FinalConfirmationResult.RolledBack(result.reason)
                is CommitResult.Failed -> FinalConfirmationResult.Failed(result.reason)
            }
        } else {
            FinalConfirmationResult.WaitingForPeer
        }
}

data class FinalConfirmationState(
    val localConfirmed: Boolean = false,
    val peerConfirmed: Boolean = false,
)

sealed interface FinalConfirmationResult {
    data object WaitingForPeer : FinalConfirmationResult

    data class Committed(val activeSlot: String) : FinalConfirmationResult

    data class RolledBack(val reason: String) : FinalConfirmationResult

    data class Failed(val reason: String) : FinalConfirmationResult

    data object Cancelled : FinalConfirmationResult
}
