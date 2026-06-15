package com.bandu.tiji.transfer.protocol.state

import com.bandu.tiji.transfer.protocol.proto.ProtocolErrorCode

enum class PairingRole {
    INITIATOR,
    RECEIVER,
}

enum class PairingPhase {
    IDLE,
    WAITING_SRP_HELLO,
    WAITING_SRP_CHALLENGE,
    WAITING_CLIENT_PROOF,
    WAITING_SERVER_PROOF,
    WAITING_IDENTITY,
    WAITING_CONFIRMATIONS,
    WAITING_LOCAL_CONFIRMATION,
    WAITING_PEER_CONFIRMATION,
    PAIRED,
    CANCELLED,
    FAILED,
}

enum class PairingEvent {
    START_INITIATOR,
    START_RECEIVER,
    RECEIVE_SRP_HELLO,
    RECEIVE_SRP_CHALLENGE,
    VERIFY_CLIENT_PROOF,
    VERIFY_SERVER_PROOF,
    VERIFY_IDENTITY,
    LOCAL_CONFIRM,
    PEER_CONFIRM,
    CANCEL,
    FAIL_AUTHENTICATION,
    EXPIRE,
}

data class PairingState(
    val phase: PairingPhase,
    val role: PairingRole? = null,
    val failureCode: ProtocolErrorCode? = null,
) {
    init {
        require((phase == PairingPhase.IDLE) == (role == null)) {
            "Only idle pairing state has no role"
        }
        require((phase == PairingPhase.FAILED) == (failureCode != null)) {
            "Only failed pairing state has a failure code"
        }
    }
}

class InvalidPairingTransitionException(
    val code: ProtocolErrorCode = ProtocolErrorCode.PROTOCOL_ERROR_CODE_INVALID_STATE,
) : IllegalStateException(code.name)

object PairingStateMachine {
    val initialState = PairingState(PairingPhase.IDLE)

    fun transition(
        state: PairingState,
        event: PairingEvent,
    ): PairingState {
        if (state.phase in ACTIVE_PHASES) {
            when (event) {
                PairingEvent.CANCEL ->
                    return PairingState(PairingPhase.CANCELLED, state.role)
                PairingEvent.FAIL_AUTHENTICATION ->
                    return PairingState(
                        PairingPhase.FAILED,
                        state.role,
                        ProtocolErrorCode.PROTOCOL_ERROR_CODE_AUTHENTICATION_FAILED,
                    )
                PairingEvent.EXPIRE ->
                    return PairingState(
                        PairingPhase.FAILED,
                        state.role,
                        ProtocolErrorCode.PROTOCOL_ERROR_CODE_PAIRING_CODE_EXPIRED,
                    )
                else -> Unit
            }
        }

        val nextPhase = when (state.phase to event) {
            PairingPhase.IDLE to PairingEvent.START_INITIATOR ->
                return PairingState(
                    PairingPhase.WAITING_SRP_CHALLENGE,
                    PairingRole.INITIATOR,
                )
            PairingPhase.IDLE to PairingEvent.START_RECEIVER ->
                return PairingState(
                    PairingPhase.WAITING_SRP_HELLO,
                    PairingRole.RECEIVER,
                )
            PairingPhase.WAITING_SRP_HELLO to PairingEvent.RECEIVE_SRP_HELLO ->
                PairingPhase.WAITING_CLIENT_PROOF
            PairingPhase.WAITING_CLIENT_PROOF to PairingEvent.VERIFY_CLIENT_PROOF ->
                PairingPhase.WAITING_IDENTITY
            PairingPhase.WAITING_SRP_CHALLENGE to PairingEvent.RECEIVE_SRP_CHALLENGE ->
                PairingPhase.WAITING_SERVER_PROOF
            PairingPhase.WAITING_SERVER_PROOF to PairingEvent.VERIFY_SERVER_PROOF ->
                PairingPhase.WAITING_IDENTITY
            PairingPhase.WAITING_IDENTITY to PairingEvent.VERIFY_IDENTITY ->
                PairingPhase.WAITING_CONFIRMATIONS
            PairingPhase.WAITING_CONFIRMATIONS to PairingEvent.LOCAL_CONFIRM ->
                PairingPhase.WAITING_PEER_CONFIRMATION
            PairingPhase.WAITING_CONFIRMATIONS to PairingEvent.PEER_CONFIRM ->
                PairingPhase.WAITING_LOCAL_CONFIRMATION
            PairingPhase.WAITING_LOCAL_CONFIRMATION to PairingEvent.LOCAL_CONFIRM ->
                PairingPhase.PAIRED
            PairingPhase.WAITING_PEER_CONFIRMATION to PairingEvent.PEER_CONFIRM ->
                PairingPhase.PAIRED
            else -> throw InvalidPairingTransitionException()
        }
        return PairingState(nextPhase, state.role)
    }

    private val ACTIVE_PHASES = setOf(
        PairingPhase.WAITING_SRP_HELLO,
        PairingPhase.WAITING_SRP_CHALLENGE,
        PairingPhase.WAITING_CLIENT_PROOF,
        PairingPhase.WAITING_SERVER_PROOF,
        PairingPhase.WAITING_IDENTITY,
        PairingPhase.WAITING_CONFIRMATIONS,
        PairingPhase.WAITING_LOCAL_CONFIRMATION,
        PairingPhase.WAITING_PEER_CONFIRMATION,
    )
}
