package com.bandu.tiji.domain.usecase.tutor

import com.bandu.tiji.domain.util.runSuspendCatching

import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.core.model.id.TutorSessionId
import com.bandu.tiji.domain.repository.TutorRepository

class DeleteTutorSessionUseCase(
    private val repository: TutorRepository,
) {
    suspend operator fun invoke(id: TutorSessionId): AppResult<Unit> =
        runSuspendCatching { repository.deleteSession(id) }
}
