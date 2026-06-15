package com.bandu.tiji.domain.repository

import com.bandu.tiji.core.model.profile.StudentProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfile(): Flow<StudentProfile>

    suspend fun updateProfile(profile: StudentProfile)

    suspend fun clearLearningData()

    suspend fun factoryReset()
}
