package com.bandu.tiji.ai.api.model

import com.bandu.tiji.core.model.enums.ExerciseDifficulty
import com.bandu.tiji.core.model.enums.GradeResult
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.model.id.ExerciseId
import com.bandu.tiji.core.model.id.TutorSessionId

data class ExerciseRequest(
    val sessionId: TutorSessionId,
    val originalQuestion: String,
    val knowledgePoints: String,
    val difficulty: ExerciseDifficulty,
    val gradeInstruction: String = "",
    val sourceErrorItemId: ErrorItemId? = null,
    val subject: String = "",
) {
    init {
        require(originalQuestion.isNotBlank()) { "originalQuestion must not be blank" }
    }
}

data class GeneratedExercise(
    val questionText: String,
    val answerText: String,
    val analysis: String,
) {
    init {
        require(questionText.isNotBlank()) { "questionText must not be blank" }
        require(answerText.isNotBlank()) { "answerText must not be blank" }
        require(analysis.isNotBlank()) { "analysis must not be blank" }
    }
}

data class GradeExerciseRequest(
    val exerciseId: ExerciseId,
    val exerciseQuestion: String,
    val expectedAnswer: String,
    val userAnswer: String,
    val rubricContext: String = "",
) {
    init {
        require(exerciseQuestion.isNotBlank()) { "exerciseQuestion must not be blank" }
        require(expectedAnswer.isNotBlank()) { "expectedAnswer must not be blank" }
        require(userAnswer.isNotBlank()) { "userAnswer must not be blank" }
    }
}

data class ExerciseGrade(
    val result: GradeResult,
    val feedback: String,
    val confidence: Double,
    val rawResult: GradeResult,
) {
    init {
        require(confidence in 0.0..1.0) { "confidence must be between 0.0 and 1.0" }
    }

    companion object {
        const val LOW_CONFIDENCE_THRESHOLD = 0.65
    }
}
