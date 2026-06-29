package com.bandu.tiji.transfer.runtime.session

import com.bandu.tiji.transfer.runtime.commit.CommitRecovery
import com.bandu.tiji.transfer.runtime.commit.TransferCommitCoordinator
import com.bandu.tiji.transfer.runtime.commit.TransferCommitGateway
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FinalTransferConfirmationCoordinatorTest {
    @Test
    fun `local confirmation waits until peer also confirms`() = runTest {
        val gateway = FakeCommitGateway()
        val coordinator = FinalTransferConfirmationCoordinator(TransferCommitCoordinator(gateway))

        val first = coordinator.localConfirm("session-a", inactiveSlot = "slot_b")

        assertThat(first).isEqualTo(FinalConfirmationResult.WaitingForPeer)
        assertThat(gateway.events).isEmpty()
        assertThat(coordinator.state("session-a").localConfirmed).isTrue()
    }

    @Test
    fun `peer confirmation after local confirmation commits inactive slot`() = runTest {
        val gateway = FakeCommitGateway()
        val coordinator = FinalTransferConfirmationCoordinator(TransferCommitCoordinator(gateway))

        coordinator.localConfirm("session-a", inactiveSlot = "slot_b")
        val result = coordinator.peerConfirm("session-a", inactiveSlot = "slot_b")

        assertThat(result).isEqualTo(FinalConfirmationResult.Committed("slot_b"))
        assertThat(gateway.events).containsExactly(
            "verify:slot_b",
            "pending:slot_b",
            "switch:slot_b",
            "clear-key",
            "finalize",
        ).inOrder()
    }

    @Test
    fun `cancel before both confirmations prevents commit`() = runTest {
        val gateway = FakeCommitGateway()
        val coordinator = FinalTransferConfirmationCoordinator(TransferCommitCoordinator(gateway))

        coordinator.peerConfirm("session-a", inactiveSlot = "slot_b")
        val result = coordinator.cancel("session-a")

        assertThat(result).isEqualTo(FinalConfirmationResult.Cancelled)
        assertThat(gateway.events).isEmpty()
        assertThat(coordinator.state("session-a")).isEqualTo(FinalConfirmationState())
    }

    @Test
    fun `rollback from commit coordinator is surfaced`() = runTest {
        val gateway = FakeCommitGateway().apply {
            recovery = CommitRecovery.RolledBack("pending slot unhealthy")
        }
        val coordinator = FinalTransferConfirmationCoordinator(TransferCommitCoordinator(gateway))

        coordinator.localConfirm("session-a", inactiveSlot = "slot_b")
        val result = coordinator.peerConfirm("session-a", inactiveSlot = "slot_b")

        assertThat(result).isEqualTo(FinalConfirmationResult.RolledBack("pending slot unhealthy"))
    }
}

private class FakeCommitGateway : TransferCommitGateway {
    val events = mutableListOf<String>()
    var recovery: CommitRecovery = CommitRecovery.Finalized("slot_b")

    override suspend fun activeSlot(): String = "slot_a"

    override suspend fun verifySlot(slotName: String) {
        events += "verify:$slotName"
    }

    override suspend fun markPendingCommit(slotName: String) {
        events += "pending:$slotName"
    }

    override suspend fun switchActiveSlot(slotName: String) {
        events += "switch:$slotName"
    }

    override fun clearApiKey() {
        events += "clear-key"
    }

    override suspend fun finalizePendingCommit(): CommitRecovery {
        events += "finalize"
        return recovery
    }
}
