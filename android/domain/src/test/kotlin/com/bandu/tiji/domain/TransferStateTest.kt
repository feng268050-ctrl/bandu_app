package com.bandu.tiji.domain

import com.bandu.tiji.core.model.transfer.TransferPhase
import com.bandu.tiji.core.model.transfer.TransferProgress
import com.bandu.tiji.domain.transfer.DiscoveryMode
import com.bandu.tiji.domain.transfer.NearbyDevice
import com.bandu.tiji.domain.transfer.TransferFailureCode
import com.bandu.tiji.domain.transfer.TransferOfferSummary
import com.bandu.tiji.domain.transfer.TransferState
import com.bandu.tiji.domain.transfer.TransferSummary
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TransferStateTest {
    @Test
    fun transferState_coversMigrationLifecycle() {
        val peer = NearbyDevice("peer-1", "Phone B", DiscoveryMode.PAIR)
        val offer = TransferOfferSummary("s1", "Phone A", 100L, 1, 10)
        val progress = TransferProgress(TransferPhase.TRANSFERRING, 50, 50L, 100L, 1024L)
        val summary = TransferSummary("s1", 100L, 1_000L)

        val states: List<TransferState> = listOf(
            TransferState.Idle,
            TransferState.Discovering,
            TransferState.Pairing(peer, expiresAt = 123L),
            TransferState.AwaitingOfferConfirmation(offer),
            TransferState.Transferring(progress),
            TransferState.Verifying(progress = 80),
            TransferState.AwaitingFinalConfirmation,
            TransferState.Committing,
            TransferState.Completed(summary),
            TransferState.Failed(TransferFailureCode.NETWORK_INTERRUPTED, resumable = true),
        )

        assertThat(states).hasSize(10)
        assertThat(states.filterIsInstance<TransferState.Failed>().single().resumable).isTrue()
    }
}
