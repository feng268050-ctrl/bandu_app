package com.bandu.tiji.domain.usecase.profile

import com.bandu.tiji.domain.util.runSuspendCatching

import com.bandu.tiji.core.common.result.AppError
import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.core.common.time.Clock
import com.bandu.tiji.core.model.profile.StudentProfile
import com.bandu.tiji.domain.repository.ProfileRepository
import java.time.Instant
import java.time.ZoneId

class UpdateStudentProfileUseCase(
    private val repository: ProfileRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(profile: StudentProfile): AppResult<Unit> {
        profile.enrollmentYear?.let { year ->
            val currentYear = Instant.ofEpochMilli(clock.nowEpochMillis())
                .atZone(ZoneId.systemDefault())
                .year
            if (year > currentYear) {
                return AppResult.Failure(AppError.Validation("profile.enrollment_year.future"))
            }
        }
        return runSuspendCatching { repository.updateProfile(profile) }
    }
}
