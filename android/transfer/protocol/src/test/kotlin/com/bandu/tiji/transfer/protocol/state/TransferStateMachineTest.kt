package com.bandu.tiji.transfer.protocol.state

import com.bandu.tiji.transfer.protocol.proto.ProtocolErrorCode
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class TransferStateMachineTest {
    @Test
    fun `source completes offer transfer verify and commit sequence`() {
        val completed = transitionAll(
            TransferStateMachine.initialState,
            TransferEvent.SEND_OFFER,
            TransferEvent.ACCEPT_OFFER,
            TransferEvent.TRANSFER_COMPLETE,
            TransferEvent.VERIFY_SUCCEEDED,
            TransferEvent.LOCAL_COMMIT_READY,
            TransferEvent.PEER_COMMIT_READY,
            TransferEvent.LOCAL_CONFIRM,
            TransferEvent.PEER_CONFIRM,
            TransferEvent.COMMIT_COMPLETE,
        )

        assertThat(completed)
            .isEqualTo(TransferProtocolState(TransferPhase.COMPLETED, TransferRole.SOURCE))
    }

    @Test
    fun `target supports peer ready and peer confirmation arriving first`() {
        val completed = transitionAll(
            TransferStateMachine.initialState,
            TransferEvent.RECEIVE_OFFER,
            TransferEvent.ACCEPT_OFFER,
            TransferEvent.TRANSFER_COMPLETE,
            TransferEvent.VERIFY_SUCCEEDED,
            TransferEvent.PEER_COMMIT_READY,
            TransferEvent.LOCAL_COMMIT_READY,
            TransferEvent.PEER_CONFIRM,
            TransferEvent.LOCAL_CONFIRM,
            TransferEvent.COMMIT_COMPLETE,
        )

        assertThat(completed)
            .isEqualTo(TransferProtocolState(TransferPhase.COMPLETED, TransferRole.TARGET))
    }

    @Test
    fun `offer rejection is terminal and cannot start transfer`() {
        val rejected = transitionAll(
            TransferStateMachine.initialState,
            TransferEvent.RECEIVE_OFFER,
            TransferEvent.REJECT_OFFER,
        )

        assertThat(rejected.phase).isEqualTo(TransferPhase.REJECTED)
        assertInvalid(rejected, TransferEvent.ACCEPT_OFFER)
        assertInvalid(rejected, TransferEvent.REJECT_OFFER)
    }

    @Test
    fun `cancel terminates every pre-commit active phase`() {
        cancellableStates().forEach { state ->
            val cancelled = TransferStateMachine.transition(state, TransferEvent.CANCEL)
            assertThat(cancelled.phase).isEqualTo(TransferPhase.CANCELLED)
            assertInvalid(cancelled, TransferEvent.CANCEL)
        }
    }

    @Test
    fun `verification failure is non resumable integrity failure`() {
        val verifying = transitionAll(
            TransferStateMachine.initialState,
            TransferEvent.SEND_OFFER,
            TransferEvent.ACCEPT_OFFER,
            TransferEvent.TRANSFER_COMPLETE,
        )

        val failed = TransferStateMachine.transition(
            verifying,
            TransferEvent.VERIFY_FAILED,
        )

        assertThat(failed.failureCode)
            .isEqualTo(ProtocolErrorCode.PROTOCOL_ERROR_CODE_INTEGRITY_FAILED)
        assertThat(failed.resumable).isFalse()
    }

    @Test
    fun `duplicate messages and events outside transition table are rejected`() {
        representativeStates().forEach { state ->
            val allowed = LEGAL_EVENTS.getValue(state.phase)
            TransferEvent.entries
                .filterNot(allowed::contains)
                .forEach { event -> assertInvalid(state, event) }
        }
    }

    private fun assertInvalid(
        state: TransferProtocolState,
        event: TransferEvent,
    ) {
        val error = assertThrows(InvalidTransferTransitionException::class.java) {
            TransferStateMachine.transition(state, event)
        }
        assertThat(error.code)
            .isEqualTo(ProtocolErrorCode.PROTOCOL_ERROR_CODE_INVALID_STATE)
    }

    private fun transitionAll(
        start: TransferProtocolState,
        vararg events: TransferEvent,
    ): TransferProtocolState = events.fold(start, TransferStateMachine::transition)

    private fun cancellableStates(): List<TransferProtocolState> =
        representativeStates().filter { it.phase in CANCELLABLE_PHASES }

    private fun representativeStates(): List<TransferProtocolState> = listOf(
        TransferProtocolState(TransferPhase.IDLE),
        TransferProtocolState(TransferPhase.OFFER_PENDING, TransferRole.SOURCE),
        TransferProtocolState(TransferPhase.TRANSFERRING, TransferRole.SOURCE),
        TransferProtocolState(TransferPhase.VERIFYING, TransferRole.TARGET),
        TransferProtocolState(TransferPhase.WAITING_COMMIT_READY, TransferRole.TARGET),
        TransferProtocolState(TransferPhase.WAITING_LOCAL_COMMIT_READY, TransferRole.TARGET),
        TransferProtocolState(TransferPhase.WAITING_PEER_COMMIT_READY, TransferRole.SOURCE),
        TransferProtocolState(TransferPhase.WAITING_FINAL_CONFIRMATIONS, TransferRole.SOURCE),
        TransferProtocolState(TransferPhase.WAITING_LOCAL_CONFIRMATION, TransferRole.TARGET),
        TransferProtocolState(TransferPhase.WAITING_PEER_CONFIRMATION, TransferRole.SOURCE),
        TransferProtocolState(TransferPhase.COMMITTING, TransferRole.TARGET),
        TransferProtocolState(TransferPhase.COMPLETED, TransferRole.TARGET),
        TransferProtocolState(TransferPhase.REJECTED, TransferRole.TARGET),
        TransferProtocolState(TransferPhase.CANCELLED, TransferRole.SOURCE),
        TransferProtocolState(
            TransferPhase.FAILED,
            TransferRole.TARGET,
            ProtocolErrorCode.PROTOCOL_ERROR_CODE_INTEGRITY_FAILED,
        ),
    )

    companion object {
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
        private val LEGAL_EVENTS = mapOf(
            TransferPhase.IDLE to setOf(
                TransferEvent.SEND_OFFER,
                TransferEvent.RECEIVE_OFFER,
            ),
            TransferPhase.OFFER_PENDING to setOf(
                TransferEvent.ACCEPT_OFFER,
                TransferEvent.REJECT_OFFER,
                TransferEvent.CANCEL,
            ),
            TransferPhase.TRANSFERRING to setOf(
                TransferEvent.TRANSFER_COMPLETE,
                TransferEvent.CANCEL,
            ),
            TransferPhase.VERIFYING to setOf(
                TransferEvent.VERIFY_SUCCEEDED,
                TransferEvent.VERIFY_FAILED,
                TransferEvent.CANCEL,
            ),
            TransferPhase.WAITING_COMMIT_READY to setOf(
                TransferEvent.LOCAL_COMMIT_READY,
                TransferEvent.PEER_COMMIT_READY,
                TransferEvent.CANCEL,
            ),
            TransferPhase.WAITING_LOCAL_COMMIT_READY to setOf(
                TransferEvent.LOCAL_COMMIT_READY,
                TransferEvent.CANCEL,
            ),
            TransferPhase.WAITING_PEER_COMMIT_READY to setOf(
                TransferEvent.PEER_COMMIT_READY,
                TransferEvent.CANCEL,
            ),
            TransferPhase.WAITING_FINAL_CONFIRMATIONS to setOf(
                TransferEvent.LOCAL_CONFIRM,
                TransferEvent.PEER_CONFIRM,
                TransferEvent.CANCEL,
            ),
            TransferPhase.WAITING_LOCAL_CONFIRMATION to setOf(
                TransferEvent.LOCAL_CONFIRM,
                TransferEvent.CANCEL,
            ),
            TransferPhase.WAITING_PEER_CONFIRMATION to setOf(
                TransferEvent.PEER_CONFIRM,
                TransferEvent.CANCEL,
            ),
            TransferPhase.COMMITTING to setOf(TransferEvent.COMMIT_COMPLETE),
            TransferPhase.COMPLETED to emptySet(),
            TransferPhase.REJECTED to emptySet(),
            TransferPhase.CANCELLED to emptySet(),
            TransferPhase.FAILED to emptySet(),
        )
    }
}
