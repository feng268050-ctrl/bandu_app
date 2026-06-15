package com.bandu.tiji.core.testing.fake

import app.cash.turbine.test
import com.bandu.tiji.core.model.transfer.TransferPhase
import com.bandu.tiji.core.model.transfer.TransferProgress
import com.bandu.tiji.domain.transfer.DiscoveryMode
import com.bandu.tiji.domain.transfer.NearbyDevice
import com.bandu.tiji.domain.transfer.PairingCode
import com.bandu.tiji.domain.transfer.PairingResult
import com.bandu.tiji.domain.transfer.TransferFailureCode
import com.bandu.tiji.domain.transfer.TransferState
import com.bandu.tiji.domain.transfer.TrustedDevice
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FakeDeviceTransferRepositoryTest {
    @Test
    fun `transfer state flow advances through controlled states`() = runTest {
        val repository = FakeDeviceTransferRepository()
        val progress =
            TransferProgress(
                phase = TransferPhase.TRANSFERRING,
                percentComplete = 50,
                transferredBytes = 50,
                totalBytes = 100,
                bytesPerSecond = 10,
            )

        repository.observeTransferState().test {
            assertThat(awaitItem()).isEqualTo(TransferState.Idle)

            repository.startDiscovery()
            assertThat(awaitItem()).isEqualTo(TransferState.Discovering)

            repository.emitTransferState(TransferState.Transferring(progress))
            assertThat(awaitItem()).isEqualTo(TransferState.Transferring(progress))

            val failure = TransferState.Failed(TransferFailureCode.NETWORK_INTERRUPTED, true)
            repository.emitTransferState(failure)
            assertThat(awaitItem()).isEqualTo(failure)

            repository.stopDiscovery()
            assertThat(awaitItem()).isEqualTo(TransferState.Idle)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `device flows and transfer commands are observable and recorded`() = runTest {
        val nearby = NearbyDevice("nearby-1", "Phone B", DiscoveryMode.PAIR)
        val trusted = TrustedDevice("trusted-1", "Phone C", "fingerprint")
        val repository = FakeDeviceTransferRepository()
        repository.pairingCode = PairingCode("654321", 1234)
        repository.pairingResult = PairingResult.Success

        repository.observeNearbyDevices().test {
            assertThat(awaitItem()).isEmpty()
            repository.emitNearbyDevices(listOf(nearby))
            assertThat(awaitItem()).containsExactly(nearby)
            cancelAndIgnoreRemainingEvents()
        }
        repository.observeTrustedDevices().test {
            assertThat(awaitItem()).isEmpty()
            repository.emitTrustedDevices(listOf(trusted))
            assertThat(awaitItem()).containsExactly(trusted)
            repository.forgetDevice(trusted.deviceId)
            assertThat(awaitItem()).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }

        assertThat(repository.createReceiveCode()).isEqualTo(PairingCode("654321", 1234))
        assertThat(repository.pair(nearby, "654321")).isEqualTo(PairingResult.Success)
        repository.sendAll(trusted)
        repository.acceptTransfer("session-1")
        repository.rejectTransfer("session-2")
        repository.cancelTransfer()

        assertThat(repository.pairRequests).containsExactly(PairRequest(nearby, "654321"))
        assertThat(repository.sendTargets).containsExactly(trusted)
        assertThat(repository.acceptedSessionIds).containsExactly("session-1")
        assertThat(repository.rejectedSessionIds).containsExactly("session-2")
        assertThat(repository.cancelTransferCalls).isEqualTo(1)
        assertThat(repository.forgottenDeviceIds).containsExactly(trusted.deviceId)
    }

    @Test
    fun `queued failure affects one transfer command`() = runTest {
        val repository = FakeDeviceTransferRepository()
        repository.failures.enqueue(IllegalStateException("discovery failed"))

        val failure = runCatching { repository.startDiscovery() }.exceptionOrNull()

        assertThat(failure).hasMessageThat().isEqualTo("discovery failed")
        assertThat(repository.observeTransferState().value).isEqualTo(TransferState.Idle)
        repository.startDiscovery()
        assertThat(repository.observeTransferState().value).isEqualTo(TransferState.Discovering)
    }
}
