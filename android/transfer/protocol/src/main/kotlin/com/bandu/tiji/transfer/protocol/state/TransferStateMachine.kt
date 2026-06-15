package com.bandu.tiji.transfer.protocol.state

import com.bandu.tiji.transfer.protocol.proto.ProtocolErrorCode

enum class TransferRole {
    SOURCE,
    TARGET,
}

enum class TransferPhase {
    IDLE,
    OFFER_PENDING,
    TRANSFERRING,
    VERIFYING,
    WAITING_COMMIT_READY,
    WAITING_LOCAL_COMMIT_READY,
    WAITING_PEER_COMMIT_READY,
    WAITING_FINAL_CONFIRMATIONS,
    WAITING_LOCAL_CONFIRMATION,
    WAITING_PEER_CONFIRMATION,
    COMMITTING,
    COMPLETED,
    REJECTED,
    CANCELLED,
    FAILED,
}

enum class TransferEvent {
    SEND_OFFER,
    RECEIVE_OFFER,
    ACCEPT_OFFER,
    REJECT_OFFER,
    TRANSFER_COMPLETE,
    TRANSFER_INTERRUPTED,
    VERIFY_SUCCEEDED,
    VERIFY_FAILED,
    LOCAL_COMMIT_READY,
    PEER_COMMIT_READY,
    LOCAL_CONFIRM,
    PEER_CONFIRM,
    COMMIT_COMPLETE,
    CANCEL,
}

data class TransferProtocolState(
    val phase: TransferPhase,
    val role: TransferRole? = null,
    val failureCode: ProtocolErrorCode? = null,
    val resumable: Boolean = false,
) {
    init {
        require((phase == TransferPhase.IDLE) == (role == null)) {
            "Only idle transfer state has no role"
        }
        require((phase == TransferPhase.FAILED) == (failureCode != null)) {
            "Only failed transfer state has a failure code"
        }
        require(!resumable || phase == TransferPhase.FAILED) {
            "Only failed transfer state may be resumable"
        }
    }
}

class InvalidTransferTransitionException(
    val code: ProtocolErrorCode = ProtocolErrorCode.PROTOCOL_ERROR_CODE_INVALID_STATE,
) : IllegalStateException(code.name)

object TransferStateMachine {
    val initialState = TransferProtocolState(TransferPhase.IDLE)

    fun transition(
        state: TransferProtocolState,
        event: TransferEvent,
    ): TransferProtocolState {
        if (state.phase in CANCELLABLE_PHASES && event == TransferEvent.CANCEL) {
            return TransferProtocolState(TransferPhase.CANCELLED, state.role)
        }

        val nextPhase = when (state.phase to event) {
            TransferPhase.IDLE to TransferEvent.SEND_OFFER ->
                return TransferProtocolState(
                    phase = TransferPhase.OFFER_PENDING,
                    role = TransferRole.SOURCE,
                )
            TransferPhase.IDLE to TransferEvent.RECEIVE_OFFER ->
                return TransferProtocolState(
                    phase = TransferPhase.OFFER_PENDING,
                    role = TransferRole.TARGET,
                )
            TransferPhase.OFFER_PENDING to TransferEvent.ACCEPT_OFFER ->
                TransferPhase.TRANSFERRING
            TransferPhase.OFFER_PENDING to TransferEvent.REJECT_OFFER ->
                return TransferProtocolState(TransferPhase.REJECTED, state.role)
            TransferPhase.TRANSFERRING to TransferEvent.TRANSFER_COMPLETE ->
                TransferPhase.VERIFYING
            TransferPhase.TRANSFERRING to TransferEvent.TRANSFER_INTERRUPTED ->
                return TransferProtocolState(
                    phase = TransferPhase.FAILED,
                    role = state.role,
                    failureCode = ProtocolErrorCode.PROTOCOL_ERROR_CODE_CONNECTION_LOST,
                    resumable = true,
                )
            TransferPhase.VERIFYING to TransferEvent.VERIFY_SUCCEEDED ->
                TransferPhase.WAITING_COMMIT_READY
            TransferPhase.VERIFYING to TransferEvent.VERIFY_FAILED ->
                return TransferProtocolState(
                    phase = TransferPhase.FAILED,
                    role = state.role,
                    failureCode = ProtocolErrorCode.PROTOCOL_ERROR_CODE_INTEGRITY_FAILED,
                    resumable = false,
                )
            TransferPhase.WAITING_COMMIT_READY to TransferEvent.LOCAL_COMMIT_READY ->
                TransferPhase.WAITING_PEER_COMMIT_READY
            TransferPhase.WAITING_COMMIT_READY to TransferEvent.PEER_COMMIT_READY ->
                TransferPhase.WAITING_LOCAL_COMMIT_READY
            TransferPhase.WAITING_LOCAL_COMMIT_READY to TransferEvent.LOCAL_COMMIT_READY ->
                TransferPhase.WAITING_FINAL_CONFIRMATIONS
            TransferPhase.WAITING_PEER_COMMIT_READY to TransferEvent.PEER_COMMIT_READY ->
                TransferPhase.WAITING_FINAL_CONFIRMATIONS
            TransferPhase.WAITING_FINAL_CONFIRMATIONS to TransferEvent.LOCAL_CONFIRM ->
                TransferPhase.WAITING_PEER_CONFIRMATION
            TransferPhase.WAITING_FINAL_CONFIRMATIONS to TransferEvent.PEER_CONFIRM ->
                TransferPhase.WAITING_LOCAL_CONFIRMATION
            TransferPhase.WAITING_LOCAL_CONFIRMATION to TransferEvent.LOCAL_CONFIRM ->
                TransferPhase.COMMITTING
            TransferPhase.WAITING_PEER_CONFIRMATION to TransferEvent.PEER_CONFIRM ->
                TransferPhase.COMMITTING
            TransferPhase.COMMITTING to TransferEvent.COMMIT_COMPLETE ->
                TransferPhase.COMPLETED
            else -> throw InvalidTransferTransitionException()
        }
        return TransferProtocolState(nextPhase, state.role)
    }

    private val CANCELLABLE_PHASES = setOf(
        TransferPhase.OFFER_PENDING,
        TransferPhase.TRANSFERRING,
        TransferPhase.VERIFYING,
        TransferPhase.WAITING_COMMIT_READY,
        TransferPhase.WAITING_LOCAL_COMMIT_READY,
        TransferPhase.WAITING_PEER_COMMIT_READY,
        TransferPhase.WAITING_FINAL_CONFIRMATIONS,
        TransferPhase.WAITING_LOCAL_CONFIRMATION,
        TransferPhase.WAITING_PEER_CONFIRMATION,
    )
}
