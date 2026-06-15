package com.bandu.tiji.transfer.protocol.state

import com.bandu.tiji.transfer.protocol.proto.ProtocolErrorCode
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class PairingStateMachineTest {
    @Test
    fun `initiator completes legal pairing sequence`() {
        val paired = transitionAll(
            PairingStateMachine.initialState,
            PairingEvent.START_INITIATOR,
            PairingEvent.RECEIVE_SRP_CHALLENGE,
            PairingEvent.VERIFY_SERVER_PROOF,
            PairingEvent.VERIFY_IDENTITY,
            PairingEvent.LOCAL_CONFIRM,
            PairingEvent.PEER_CONFIRM,
        )

        assertThat(paired)
            .isEqualTo(PairingState(PairingPhase.PAIRED, PairingRole.INITIATOR))
    }

    @Test
    fun `receiver completes legal pairing sequence with peer confirming first`() {
        val paired = transitionAll(
            PairingStateMachine.initialState,
            PairingEvent.START_RECEIVER,
            PairingEvent.RECEIVE_SRP_HELLO,
            PairingEvent.VERIFY_CLIENT_PROOF,
            PairingEvent.VERIFY_IDENTITY,
            PairingEvent.PEER_CONFIRM,
            PairingEvent.LOCAL_CONFIRM,
        )

        assertThat(paired)
            .isEqualTo(PairingState(PairingPhase.PAIRED, PairingRole.RECEIVER))
    }

    @Test
    fun `cancel failure and expiration terminate every active phase`() {
        activeStates().forEach { state ->
            assertThat(PairingStateMachine.transition(state, PairingEvent.CANCEL).phase)
                .isEqualTo(PairingPhase.CANCELLED)
            assertThat(
                PairingStateMachine.transition(
                    state,
                    PairingEvent.FAIL_AUTHENTICATION,
                ).failureCode,
            ).isEqualTo(ProtocolErrorCode.PROTOCOL_ERROR_CODE_AUTHENTICATION_FAILED)
            assertThat(PairingStateMachine.transition(state, PairingEvent.EXPIRE).failureCode)
                .isEqualTo(ProtocolErrorCode.PROTOCOL_ERROR_CODE_PAIRING_CODE_EXPIRED)
        }
    }

    @Test
    fun `all events outside the legal transition table are rejected`() {
        representativeStates().forEach { state ->
            val allowed = LEGAL_EVENTS.getValue(state.phase)
            PairingEvent.entries
                .filterNot(allowed::contains)
                .forEach { event ->
                    val error = assertThrows(InvalidPairingTransitionException::class.java) {
                        PairingStateMachine.transition(state, event)
                    }
                    assertThat(error.code)
                        .isEqualTo(ProtocolErrorCode.PROTOCOL_ERROR_CODE_INVALID_STATE)
                }
        }
    }

    private fun transitionAll(
        start: PairingState,
        vararg events: PairingEvent,
    ): PairingState = events.fold(start, PairingStateMachine::transition)

    private fun activeStates(): List<PairingState> =
        representativeStates().filter { it.phase in ACTIVE_PHASES }

    private fun representativeStates(): List<PairingState> = listOf(
        PairingState(PairingPhase.IDLE),
        PairingState(PairingPhase.WAITING_SRP_HELLO, PairingRole.RECEIVER),
        PairingState(PairingPhase.WAITING_SRP_CHALLENGE, PairingRole.INITIATOR),
        PairingState(PairingPhase.WAITING_CLIENT_PROOF, PairingRole.RECEIVER),
        PairingState(PairingPhase.WAITING_SERVER_PROOF, PairingRole.INITIATOR),
        PairingState(PairingPhase.WAITING_IDENTITY, PairingRole.INITIATOR),
        PairingState(PairingPhase.WAITING_CONFIRMATIONS, PairingRole.INITIATOR),
        PairingState(PairingPhase.WAITING_LOCAL_CONFIRMATION, PairingRole.INITIATOR),
        PairingState(PairingPhase.WAITING_PEER_CONFIRMATION, PairingRole.INITIATOR),
        PairingState(PairingPhase.PAIRED, PairingRole.INITIATOR),
        PairingState(PairingPhase.CANCELLED, PairingRole.INITIATOR),
        PairingState(
            PairingPhase.FAILED,
            PairingRole.INITIATOR,
            ProtocolErrorCode.PROTOCOL_ERROR_CODE_AUTHENTICATION_FAILED,
        ),
    )

    companion object {
        private val TERMINATION_EVENTS = setOf(
            PairingEvent.CANCEL,
            PairingEvent.FAIL_AUTHENTICATION,
            PairingEvent.EXPIRE,
        )
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
        private val LEGAL_EVENTS = mapOf(
            PairingPhase.IDLE to setOf(
                PairingEvent.START_INITIATOR,
                PairingEvent.START_RECEIVER,
            ),
            PairingPhase.WAITING_SRP_HELLO to
                setOf(PairingEvent.RECEIVE_SRP_HELLO) + TERMINATION_EVENTS,
            PairingPhase.WAITING_SRP_CHALLENGE to
                setOf(PairingEvent.RECEIVE_SRP_CHALLENGE) + TERMINATION_EVENTS,
            PairingPhase.WAITING_CLIENT_PROOF to
                setOf(PairingEvent.VERIFY_CLIENT_PROOF) + TERMINATION_EVENTS,
            PairingPhase.WAITING_SERVER_PROOF to
                setOf(PairingEvent.VERIFY_SERVER_PROOF) + TERMINATION_EVENTS,
            PairingPhase.WAITING_IDENTITY to
                setOf(PairingEvent.VERIFY_IDENTITY) + TERMINATION_EVENTS,
            PairingPhase.WAITING_CONFIRMATIONS to
                setOf(PairingEvent.LOCAL_CONFIRM, PairingEvent.PEER_CONFIRM) +
                TERMINATION_EVENTS,
            PairingPhase.WAITING_LOCAL_CONFIRMATION to
                setOf(PairingEvent.LOCAL_CONFIRM) + TERMINATION_EVENTS,
            PairingPhase.WAITING_PEER_CONFIRMATION to
                setOf(PairingEvent.PEER_CONFIRM) + TERMINATION_EVENTS,
            PairingPhase.PAIRED to emptySet(),
            PairingPhase.CANCELLED to emptySet(),
            PairingPhase.FAILED to emptySet(),
        )
    }
}
