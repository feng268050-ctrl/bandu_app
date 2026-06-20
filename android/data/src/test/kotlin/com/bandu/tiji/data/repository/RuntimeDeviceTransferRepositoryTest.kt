package com.bandu.tiji.data.repository

import app.cash.turbine.test
import com.bandu.tiji.domain.transfer.PairingResult
import com.bandu.tiji.domain.transfer.TransferFailureCode
import com.bandu.tiji.domain.transfer.TransferState
import com.bandu.tiji.transfer.runtime.TransferRuntime
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RuntimeDeviceTransferRepositoryTest {
    @Test
    fun `runtime state and commands are exposed through domain repository`() = runTest {
        val repository = RuntimeDeviceTransferRepository(TransferRuntime())
        val nearby = repository.observeNearbyDevices()

        repository.observeTransferState().test {
            assertThat(awaitItem()).isEqualTo(TransferState.Idle)
            repository.startDiscovery()
            assertThat(awaitItem()).isEqualTo(TransferState.Discovering)
            repository.stopDiscovery()
            assertThat(awaitItem()).isEqualTo(TransferState.Idle)
            cancelAndIgnoreRemainingEvents()
        }

        val device = nearby.first().single()
        val code = repository.createReceiveCode()
        assertThat(code.code).hasLength(6)
        assertThat(repository.pair(device, code.code)).isEqualTo(PairingResult.Success)

        repository.observeTrustedDevices().test {
            val trusted = awaitItem().single()
            repository.sendAll(trusted)
            assertThat(repository.observeTransferState().value)
                .isInstanceOf(TransferState.Completed::class.java)

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
        val repository = RuntimeDeviceTransferRepository(TransferRuntime())
        val device = repository.observeNearbyDevices().first().single()

        assertThat(repository.pair(device, "123")).isEqualTo(
            PairingResult.Failure(TransferFailureCode.PAIRING_FAILED),
        )
    }
}
