package com.bandu.tiji.domain.pending

import com.bandu.tiji.core.model.enums.ExerciseDifficulty
import com.bandu.tiji.core.model.id.ExerciseId
import com.bandu.tiji.core.model.id.TutorSessionId

sealed interface PendingAiOperation {
    data class AnalyzeCapture(val draftId: String) : PendingAiOperation

    data class SendTutorMessage(
        val sessionId: TutorSessionId,
        val text: String,
    ) : PendingAiOperation

    data class GenerateExercise(
        val sessionId: TutorSessionId,
        val difficulty: ExerciseDifficulty,
    ) : PendingAiOperation

    data class GradeExercise(
        val exerciseId: ExerciseId,
        val userAnswer: String,
    ) : PendingAiOperation
}
