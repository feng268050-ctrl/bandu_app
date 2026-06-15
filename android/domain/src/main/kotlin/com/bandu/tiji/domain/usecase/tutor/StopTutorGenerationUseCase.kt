package com.bandu.tiji.domain.usecase.tutor

import com.bandu.tiji.domain.util.runSuspendCatching

import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.core.model.id.TutorSessionId
import com.bandu.tiji.domain.repository.TutorRepository

class StopTutorGenerationUseCase(
    private val repository: TutorRepository,
) {
    suspend operator fun invoke(sessionId: TutorSessionId, partialText: String): AppResult<Unit> =
        runSuspendCatching { repository.appendAssistantMessage(sessionId, partialText) }
}
