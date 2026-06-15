package com.bandu.tiji.core.testing.fake

import com.bandu.tiji.core.model.profile.StudentProfile
import com.bandu.tiji.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeProfileRepository(
    initialProfile: StudentProfile = emptyProfile(),
) : ProfileRepository {
    private val profile = MutableStateFlow(initialProfile)

    val failures = FailureInjector()
    var clearLearningDataCalls: Int = 0
        private set
    var factoryResetCalls: Int = 0
        private set

    override fun observeProfile(): Flow<StudentProfile> = profile.asStateFlow()

    override suspend fun updateProfile(profile: StudentProfile) {
        failures.throwIfQueued()
        this.profile.value = profile
    }

    override suspend fun clearLearningData() {
        failures.throwIfQueued()
        clearLearningDataCalls += 1
    }

    override suspend fun factoryReset() {
        failures.throwIfQueued()
        factoryResetCalls += 1
        profile.value = emptyProfile()
    }

    fun emit(profile: StudentProfile) {
        this.profile.value = profile
    }

    companion object {
        fun emptyProfile(): StudentProfile =
            StudentProfile(
                nickname = "",
                educationStage = null,
                enrollmentYear = null,
            )
    }
}
