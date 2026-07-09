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

    @Test
    fun `avatar color and image changes save only after confirmation`() = runTest {
        val avatarStore = RecordingAvatarStore(
            ProfileAvatarPreferences(
                backgroundIndex = ProfileViewModel.AVATAR_BACKGROUND_COUNT - 1,
                imageUri = "content://old-avatar",
            ),
        )
        val viewModel = ProfileViewModel(
            profileRepository = FakeProfileRepository(),
            avatarStore = avatarStore,
        )
        advanceUntilIdle()

        viewModel.onAction(ProfileAction.OpenAvatarEditor)
        advanceUntilIdle()
        viewModel.onAction(ProfileAction.SelectAvatarBackground(0))
        advanceUntilIdle()
        assertThat(avatarStore.savedAvatars).isEmpty()
        assertThat(viewModel.uiState.value.avatar.imageUri).isEqualTo("content://old-avatar")
        assertThat(viewModel.uiState.value.avatarEditor?.draft).isEqualTo(
            ProfileAvatarUiState(backgroundIndex = 0, imageUri = null),
        )

        viewModel.onAction(ProfileAction.ConfirmAvatar)
        advanceUntilIdle()
        assertThat(avatarStore.savedAvatars).containsExactly(
            ProfileAvatarPreferences(backgroundIndex = 0, imageUri = null),
        )

        viewModel.onAction(ProfileAction.OpenAvatarEditor)
        advanceUntilIdle()
        viewModel.onAction(ProfileAction.UpdateAvatarImage("content://new-avatar"))
        advanceUntilIdle()
        assertThat(avatarStore.savedAvatars).hasSize(1)
        assertThat(viewModel.uiState.value.avatarEditor?.draft).isEqualTo(
            ProfileAvatarUiState(backgroundIndex = 0, imageUri = "content://new-avatar"),
        )

        viewModel.onAction(ProfileAction.ConfirmAvatar)
        advanceUntilIdle()
        assertThat(avatarStore.savedAvatars).containsExactly(
            ProfileAvatarPreferences(backgroundIndex = 0, imageUri = null),
            ProfileAvatarPreferences(backgroundIndex = 0, imageUri = "content://new-avatar"),
        )
        assertThat(viewModel.uiState.value.avatar).isEqualTo(
            ProfileAvatarUiState(backgroundIndex = 0, imageUri = "content://new-avatar"),
        )
        assertThat(viewModel.uiState.value.avatarEditor).isNull()
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

private class RecordingAvatarStore(
    initialAvatar: ProfileAvatarPreferences,
) : ProfileAvatarStore {
    private val avatar = MutableStateFlow(initialAvatar)
    val savedAvatars = mutableListOf<ProfileAvatarPreferences>()

    override fun observeAvatar() = avatar

    override suspend fun saveAvatar(avatar: ProfileAvatarPreferences) {
        savedAvatars += avatar
        this.avatar.value = avatar
    }
}
