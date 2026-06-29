package com.bandu.tiji.transfer.runtime.commit

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TransferCommitCoordinatorTest {
    @Test
    fun `commit marks pending switches slot clears api key and finalizes`() = runTest {
        val gateway = FakeCommitGateway(active = "slot_a")
        val coordinator = TransferCommitCoordinator(gateway)

        val result = coordinator.commitVerifiedImport("session-a")

        assertThat(result).isEqualTo(CommitResult.Committed("slot_b"))
        assertThat(gateway.events).containsExactly(
            "active",
            "verify:slot_b",
            "pending:slot_b",
            "switch:slot_b",
            "clear-key",
            "finalize",
        ).inOrder()
        assertThat(gateway.active).isEqualTo("slot_b")
        assertThat(gateway.apiKeyCleared).isTrue()
    }

    @Test
    fun `verification failure does not switch slot or clear api key`() = runTest {
        val gateway = FakeCommitGateway(active = "slot_a").apply {
            failVerification = true
        }
        val coordinator = TransferCommitCoordinator(gateway)

        val result = coordinator.commitVerifiedImport("session-a")

        assertThat(result).isInstanceOf(CommitResult.Failed::class.java)
        assertThat(gateway.events).containsExactly(
            "active",
            "verify:slot_b",
        ).inOrder()
        assertThat(gateway.active).isEqualTo("slot_a")
        assertThat(gateway.apiKeyCleared).isFalse()
    }

    @Test
    fun `finalize rollback reports rollback without hiding failure`() = runTest {
        val gateway = FakeCommitGateway(active = "slot_a").apply {
            recovery = CommitRecovery.RolledBack("health check failed")
        }
        val coordinator = TransferCommitCoordinator(gateway)

        val result = coordinator.commitVerifiedImport("session-a")

        assertThat(result).isEqualTo(CommitResult.RolledBack("health check failed"))
        assertThat(gateway.apiKeyCleared).isTrue()
    }

    @Test
    fun `inactive slot is the opposite of active slot`() = runTest {
        assertThat(TransferCommitCoordinator(FakeCommitGateway("slot_a")).inactiveSlot())
            .isEqualTo("slot_b")
        assertThat(TransferCommitCoordinator(FakeCommitGateway("slot_b")).inactiveSlot())
            .isEqualTo("slot_a")
    }
}

private class FakeCommitGateway(
    var active: String,
) : TransferCommitGateway {
    val events = mutableListOf<String>()
    var apiKeyCleared = false
    var failVerification = false
    var recovery: CommitRecovery = CommitRecovery.Finalized("slot_b")

    override suspend fun activeSlot(): String {
        events += "active"
        return active
    }

    override suspend fun verifySlot(slotName: String) {
        events += "verify:$slotName"
        if (failVerification) error("slot verification failed")
    }

    override suspend fun markPendingCommit(slotName: String) {
        events += "pending:$slotName"
    }

    override suspend fun switchActiveSlot(slotName: String) {
        events += "switch:$slotName"
        active = slotName
    }

    override fun clearApiKey() {
        events += "clear-key"
        apiKeyCleared = true
    }

    override suspend fun finalizePendingCommit(): CommitRecovery {
        events += "finalize"
        return recovery
    }
}
