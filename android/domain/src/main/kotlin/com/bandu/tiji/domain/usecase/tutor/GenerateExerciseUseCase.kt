package com.bandu.tiji.domain.usecase.tutor

import com.bandu.tiji.domain.util.runSuspendCatching

import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.domain.ai.ExerciseRequest
import com.bandu.tiji.domain.ai.GeneratedExercise
import com.bandu.tiji.domain.repository.AiTutorGateway

class GenerateExerciseUseCase(
    private val gateway: AiTutorGateway,
) {
    suspend operator fun invoke(request: ExerciseRequest): AppResult<GeneratedExercise> =
        runSuspendCatching { gateway.generateExercise(request) }
}
