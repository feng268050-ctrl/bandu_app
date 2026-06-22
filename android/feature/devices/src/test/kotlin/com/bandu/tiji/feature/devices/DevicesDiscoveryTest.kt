package com.bandu.tiji.feature.devices

import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeDeviceTransferRepository
import com.bandu.tiji.domain.transfer.TransferState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DevicesDiscoveryTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `start stop and page leave issue explicit repository commands`() = runTest {
        val repository = FakeDeviceTransferRepository()
        val viewModel = DevicesViewModel(repository, "本机", "fingerprint")
        advanceUntilIdle()

        viewModel.onAction(DevicesAction.StartDiscovery)
        advanceUntilIdle()
        assertThat(repository.startDiscoveryCalls).isEqualTo(1)
        assertThat(viewModel.uiState.value.transferState).isEqualTo(TransferState.Discovering)

        viewModel.onAction(DevicesAction.StopDiscovery)
        advanceUntilIdle()
        assertThat(repository.stopDiscoveryCalls).isEqualTo(1)
        assertThat(viewModel.uiState.value.transferState).isEqualTo(TransferState.Idle)

        viewModel.onAction(DevicesAction.StartDiscovery)
        advanceUntilIdle()
        viewModel.onAction(DevicesAction.LeavePage)
        advanceUntilIdle()
        assertThat(repository.stopDiscoveryCalls).isEqualTo(2)
        assertThat(viewModel.uiState.value.transferState).isEqualTo(TransferState.Idle)
    }

    @Test
    fun `discovery failure is retryable`() = runTest {
        val repository = FakeDeviceTransferRepository()
        repository.failures.enqueue(IllegalStateException("nsd"))
        val viewModel = DevicesViewModel(repository, "本机", "fingerprint")
        advanceUntilIdle()

        viewModel.onAction(DevicesAction.StartDiscovery)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.commandErrorMessage)
            .isEqualTo("无法开始发现设备")
        assertThat(viewModel.uiState.value.isDiscoveryCommandRunning).isFalse()
    }
}
