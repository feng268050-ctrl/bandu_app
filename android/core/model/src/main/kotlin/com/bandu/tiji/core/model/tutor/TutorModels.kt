package com.bandu.tiji.core.model.tutor

import com.bandu.tiji.core.model.enums.ExerciseDifficulty
import com.bandu.tiji.core.model.enums.GradeResult
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.model.id.ExerciseId
import com.bandu.tiji.core.model.id.TutorMessageId
import com.bandu.tiji.core.model.id.TutorSessionId

data class TutorSessionSummary(
    val id: TutorSessionId,
    val title: String,
    val errorItemId: ErrorItemId?,
    val updatedAtEpochMillis: Long,
)

data class TutorSession(
    val id: TutorSessionId,
    val title: String,
    val errorItemId: ErrorItemId?,
    val messages: List<TutorMessage>,
    val exercises: List<Exercise>,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

data class TutorMessage(
    val id: TutorMessageId,
    val role: TutorMessageRole,
    val content: String,
    val status: TutorMessageStatus,
    val sequence: Int,
    val createdAtEpochMillis: Long,
)

enum class TutorMessageRole {
    USER,
    ASSISTANT,
    SYSTEM_LOCAL,
}

enum class TutorMessageStatus {
    COMPLETE,
    STOPPED,
    FAILED,
}

data class Exercise(
    val id: ExerciseId,
    val sessionId: TutorSessionId,
    val sourceErrorItemId: ErrorItemId?,
    val subject: String,
    val difficulty: ExerciseDifficulty,
    val questionText: String,
    val expectedAnswer: String,
    val analysis: String,
    val userAnswer: String?,
    val aiResult: GradeResult?,
    val finalResult: GradeResult?,
    val gradingFeedback: String?,
    val gradedAtEpochMillis: Long?,
    val overriddenAtEpochMillis: Long?,
    val createdAtEpochMillis: Long,
) {
    val effectiveResult: GradeResult? = finalResult ?: aiResult
}

data class ExerciseDraft(
    val sessionId: TutorSessionId,
    val sourceErrorItemId: ErrorItemId?,
    val subject: String,
    val difficulty: ExerciseDifficulty,
    val questionText: String,
    val expectedAnswer: String,
    val analysis: String,
) {
    init {
        require(subject.isNotBlank()) { "subject must not be blank" }
        require(questionText.isNotBlank()) { "questionText must not be blank" }
        require(expectedAnswer.isNotBlank()) { "expectedAnswer must not be blank" }
        require(analysis.isNotBlank()) { "analysis must not be blank" }
    }
}
