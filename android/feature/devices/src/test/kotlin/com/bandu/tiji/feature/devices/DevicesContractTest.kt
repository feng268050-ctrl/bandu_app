package com.bandu.tiji.feature.devices

import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeDeviceTransferRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DevicesContractTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `route state starts with local identity`() {
        val viewModel = DevicesViewModel(
            repository = FakeDeviceTransferRepository(),
            localDeviceName = "我的手机",
            localFingerprint = "AA:BB:CC",
        )

        assertThat(viewModel.uiState.value.localDeviceName).isEqualTo("我的手机")
        assertThat(viewModel.uiState.value.localFingerprint).isEqualTo("AA:BB:CC")
    }

    @Test
    fun `local device name follows profile device name updates`() = runTest {
        val localDeviceName = MutableStateFlow("我的手机")
        val viewModel = DevicesViewModel(
            repository = FakeDeviceTransferRepository(),
            localDeviceName = "旧名称",
            localFingerprint = "AA:BB:CC",
            localDeviceNameUpdates = localDeviceName,
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.localDeviceName).isEqualTo("我的手机")

        localDeviceName.value = "新名称"
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.localDeviceName).isEqualTo("新名称")
    }
}
