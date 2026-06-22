package com.bandu.tiji.feature.profile

import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeProfileRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceNameViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `device name is length limited and saved immediately`() = runTest {
        val store = RecordingDeviceNameStore("旧设备")
        val viewModel = ProfileViewModel(
            profileRepository = FakeProfileRepository(),
            deviceNameStore = store,
        )
        advanceUntilIdle()

        val tooLong = "设".repeat(ProfileViewModel.MAX_DEVICE_NAME_LENGTH + 3)
        viewModel.onAction(ProfileAction.UpdateDeviceName(tooLong))
        advanceUntilIdle()

        val expected = "设".repeat(ProfileViewModel.MAX_DEVICE_NAME_LENGTH)
        assertThat(viewModel.uiState.value.deviceName).isEqualTo(expected)
        assertThat(store.savedNames).containsExactly(expected)
    }
}

private class RecordingDeviceNameStore(initialName: String) : DeviceNameStore {
    private val name = MutableStateFlow(initialName)
    val savedNames = mutableListOf<String>()

    override fun observeDeviceName() = name

    override suspend fun saveDeviceName(name: String) {
        savedNames += name
        this.name.value = name
    }
}
