package com.bandu.tiji

import com.bandu.tiji.core.storage.preferences.DevicePreferencesStore
import com.bandu.tiji.core.storage.preferences.PortablePreferencesStore
import com.bandu.tiji.core.storage.device.AndroidDeviceNameResolver
import com.bandu.tiji.feature.profile.DeviceNameStore
import com.bandu.tiji.feature.profile.ProfileAvatarPreferences
import com.bandu.tiji.feature.profile.ProfileAvatarStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class DevicePreferencesDeviceNameStore(
    private val preferences: DevicePreferencesStore,
    private val deviceNameResolver: AndroidDeviceNameResolver,
) : DeviceNameStore {
    override fun observeDeviceName() =
        preferences.data
            .map { preferences ->
                if (deviceNameResolver.shouldReplaceStoredName(preferences.deviceDisplayName)) {
                    deviceNameResolver.resolve()
                } else {
                    preferences.deviceDisplayName
                        .ifBlank { deviceNameResolver.resolve() }
                }
            }
            .distinctUntilChanged()

    override suspend fun saveDeviceName(name: String) {
        val normalized = AndroidDeviceNameResolver.normalizeDeviceName(name)
            ?: deviceNameResolver.resolve()
        val current = preferences.data.first()
        preferences.replace(
            current.copy(
                deviceDisplayName = normalized,
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
