package com.bandu.tiji.domain.usecase.tutor

import com.bandu.tiji.domain.util.runSuspendCatching

import com.bandu.tiji.core.common.result.AppError
import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.domain.ai.ExerciseGrade
import com.bandu.tiji.domain.ai.GradeExerciseRequest
import com.bandu.tiji.domain.repository.AiTutorGateway

class GradeExerciseUseCase(
    private val gateway: AiTutorGateway,
) {
    suspend operator fun invoke(request: GradeExerciseRequest): AppResult<ExerciseGrade> {
        if (request.userAnswer.isBlank()) {
            return AppResult.Failure(AppError.Validation("exercise.answer.blank"))
        }
        return runSuspendCatching { gateway.gradeExercise(request) }
    }
}
