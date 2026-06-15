package com.bandu.tiji.domain.usecase

import com.bandu.tiji.core.common.result.AppError
import com.bandu.tiji.core.model.profile.StudentProfile
import com.bandu.tiji.domain.fake.FakeProfileRepository
import com.bandu.tiji.domain.usecase.profile.ClearLearningDataUseCase
import com.bandu.tiji.domain.usecase.profile.FactoryResetUseCase
import com.bandu.tiji.domain.usecase.profile.UpdateStudentProfileUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ProfileUseCaseTest {
    private val repository = FakeProfileRepository()

    private class FixedClock(private val millis: Long) : com.bandu.tiji.core.common.time.Clock {
        override fun nowEpochMillis(): Long = millis
    }

    @Test
    fun clearLearningData_onlyClearsLearningScope() = runTest {
        val result = ClearLearningDataUseCase(repository).invoke()

        assertThat(result.isSuccess).isTrue()
        assertThat(repository.learningDataCleared).isTrue()
        assertThat(repository.factoryResetCalled).isFalse()
    }

    @Test
    fun factoryReset_wipesEverything() = runTest {
        val result = FactoryResetUseCase(repository).invoke()

        assertThat(result.isSuccess).isTrue()
        assertThat(repository.factoryResetCalled).isTrue()
        assertThat(repository.learningDataCleared).isFalse()
    }

    @Test
    fun updateStudentProfile_rejectsFutureEnrollmentYear() = runTest {
        val clock = FixedClock(millis = java.time.LocalDate.of(2026, 6, 1)
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli())

        val result = UpdateStudentProfileUseCase(repository, clock).invoke(
            StudentProfile(nickname = "小明", educationStage = "初中", enrollmentYear = 2027),
        )

        assertThat(result.isSuccess).isFalse()
        val error = (result as com.bandu.tiji.core.common.result.AppResult.Failure).error
        assertThat((error as AppError.Validation).code).isEqualTo("profile.enrollment_year.future")
    }
}
