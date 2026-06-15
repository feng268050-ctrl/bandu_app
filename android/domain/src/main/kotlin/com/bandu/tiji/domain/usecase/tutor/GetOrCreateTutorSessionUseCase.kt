package com.bandu.tiji.domain.usecase.tutor

import com.bandu.tiji.domain.util.runSuspendCatching

import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.model.id.TutorSessionId
import com.bandu.tiji.domain.repository.TutorRepository

class GetOrCreateTutorSessionUseCase(
    private val repository: TutorRepository,
) {
    suspend operator fun invoke(errorItemId: ErrorItemId? = null): AppResult<TutorSessionId> =
        runSuspendCatching { repository.getOrCreate(errorItemId) }
}
