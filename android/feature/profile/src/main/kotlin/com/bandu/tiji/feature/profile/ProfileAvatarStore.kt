package com.bandu.tiji.feature.profile

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

data class ProfileAvatarPreferences(
    val backgroundIndex: Int = 0,
    val imageUri: String? = null,
)

interface ProfileAvatarStore {
    fun observeAvatar(): Flow<ProfileAvatarPreferences>

    suspend fun saveAvatar(avatar: ProfileAvatarPreferences)
}

internal class InMemoryProfileAvatarStore(
    initialAvatar: ProfileAvatarPreferences = ProfileAvatarPreferences(),
) : ProfileAvatarStore {
    private val avatar = MutableStateFlow(initialAvatar)

    override fun observeAvatar(): Flow<ProfileAvatarPreferences> = avatar

    override suspend fun saveAvatar(avatar: ProfileAvatarPreferences) {
        this.avatar.value = avatar
    }
}
