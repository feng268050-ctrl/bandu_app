package com.bandu.tiji.feature.devices

import com.bandu.tiji.core.testing.fake.FakeDeviceTransferRepository
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DevicesContractTest {
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
}
