package com.bandu.tiji

import com.bandu.tiji.core.storage.preferences.DevicePreferencesStore
import com.bandu.tiji.core.storage.preferences.PortablePreferencesStore
import com.bandu.tiji.feature.profile.DeviceNameStore
import com.bandu.tiji.feature.profile.ProfileAvatarPreferences
import com.bandu.tiji.feature.profile.ProfileAvatarStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal class DevicePreferencesDeviceNameStore(
    private val preferences: DevicePreferencesStore,
    private val fallbackName: String,
) : DeviceNameStore {
    override fun observeDeviceName() =
        preferences.data.map { it.deviceDisplayName.ifBlank { fallbackName } }

    override suspend fun saveDeviceName(name: String) {
        val current = preferences.data.first()
        preferences.replace(
            current.copy(
                deviceDisplayName = name.ifBlank { fallbackName },
            ),
        )
    }
}

internal class PortableProfileAvatarStore(
    private val preferences: PortablePreferencesStore,
) : ProfileAvatarStore {
    override fun observeAvatar() =
        preferences.data.map {
            ProfileAvatarPreferences(
                backgroundIndex = it.avatarBackgroundIndex,
                imageUri = it.avatarImageUri,
            )
        }

    override suspend fun saveAvatar(avatar: ProfileAvatarPreferences) {
        val current = preferences.data.first()
        preferences.replace(
            current.copy(
                avatarBackgroundIndex = avatar.backgroundIndex,
                avatarImageUri = avatar.imageUri,
            ),
        )
    }
}
