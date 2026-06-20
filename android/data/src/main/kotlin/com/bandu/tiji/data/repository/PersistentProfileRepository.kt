package com.bandu.tiji.data.repository

import com.bandu.tiji.core.model.profile.StudentProfile
import com.bandu.tiji.core.storage.preferences.PortablePreferencesStore
import com.bandu.tiji.data.profile.LearningDataResetCoordinator
import com.bandu.tiji.domain.repository.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class PersistentProfileRepository @Inject constructor(
    private val portablePreferences: PortablePreferencesStore,
    private val resetCoordinator: LearningDataResetCoordinator,
) : ProfileRepository {
    override fun observeProfile(): Flow<StudentProfile> =
        portablePreferences.data.map { preferences ->
            StudentProfile(
                nickname = preferences.nickname,
                educationStage = preferences.educationStage,
                enrollmentYear = preferences.enrollmentYear,
            )
        }

    override suspend fun updateProfile(profile: StudentProfile) {
        val current = portablePreferences.data.first()
        portablePreferences.replace(
            current.copy(
                nickname = profile.nickname,
                educationStage = profile.educationStage,
                enrollmentYear = profile.enrollmentYear,
            ),
        )
    }

    override suspend fun clearLearningData() {
        resetCoordinator.clearLearningData()
    }

    override suspend fun factoryReset() {
        resetCoordinator.factoryReset()
    }
}
