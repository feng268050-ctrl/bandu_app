package com.bandu.tiji.data.repository

import app.cash.turbine.test
import com.bandu.tiji.domain.transfer.PairingResult
import com.bandu.tiji.domain.transfer.TransferFailureCode
import com.bandu.tiji.domain.transfer.TransferState
import com.bandu.tiji.transfer.runtime.TransferRuntime
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RuntimeDeviceTransferRepositoryTest {
    @Test
    fun `runtime state and commands are exposed through domain repository`() = runTest {
        val runtime = TransferRuntime()
        val repository = RuntimeDeviceTransferRepository(runtime)
        val nearby = repository.observeNearbyDevices()

        repository.observeTransferState().test {
            assertThat(awaitItem()).isEqualTo(TransferState.Idle)
            repository.startDiscovery()
            assertThat(awaitItem()).isEqualTo(TransferState.Discovering)
            repository.stopDiscovery()
            assertThat(awaitItem()).isEqualTo(TransferState.Idle)
            cancelAndIgnoreRemainingEvents()
        }

        repository.startDiscovery()
        val device = runtime.addManualDiscoveryTarget("10.0.2.2")
        assertThat(nearby.value).containsExactly(device)
        val code = repository.createReceiveCode()
        assertThat(code.code).hasLength(6)
        assertThat(repository.pair(device, code.code)).isEqualTo(PairingResult.Success)

        repository.observeTrustedDevices().test {
            val trusted = awaitItem().single()
            repository.sendAll(trusted)
            assertThat(repository.observeTransferState().value)
                .isInstanceOf(TransferState.Transferring::class.java)

            repository.rejectTransfer("session-1")
            assertThat(repository.observeTransferState().value).isEqualTo(
                TransferState.Failed(TransferFailureCode.OFFER_REJECTED, resumable = false),
            )

            repository.cancelTransfer()
            assertThat(repository.observeTransferState().value).isEqualTo(
                TransferState.Failed(TransferFailureCode.CANCELLED, resumable = false),
            )

            repository.forgetDevice(trusted.deviceId)
            assertThat(awaitItem()).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `invalid pairing code preserves failure result`() = runTest {
        val runtime = TransferRuntime()
        val repository = RuntimeDeviceTransferRepository(runtime)

        repository.startDiscovery()
        val device = runtime.addManualDiscoveryTarget("10.0.2.2")
        repository.createReceiveCode()

        assertThat(repository.pair(device, "123")).isEqualTo(
            PairingResult.Failure(TransferFailureCode.PAIRING_FAILED),
        )
    }
}
