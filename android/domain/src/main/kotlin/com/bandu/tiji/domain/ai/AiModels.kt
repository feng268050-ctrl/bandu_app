package com.bandu.tiji.domain.ai

import com.bandu.tiji.core.model.enums.AiProviderType
import com.bandu.tiji.core.model.enums.ExerciseDifficulty
import com.bandu.tiji.core.model.enums.GradeResult
import com.bandu.tiji.core.model.enums.MistakeStatus
import com.bandu.tiji.core.model.erroritem.StoredImage
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.model.id.ExerciseId
import com.bandu.tiji.core.model.id.TutorSessionId

data class AiConfiguration(
    val id: String,
    val displayName: String,
    val providerType: AiProviderType,
    val baseUrl: String,
    val analysisModel: String,
    val tutorModel: String,
    val hasApiKey: Boolean,
)

data class AiConfigurationDraft(
    val providerType: AiProviderType,
    val displayName: String,
    val baseUrl: String,
    val apiKey: String?,
    val analysisModel: String,
    val tutorModel: String,
    val allowPrivateCleartext: Boolean = false,
)

sealed interface ValidationResult {
    data object Success : ValidationResult

    data class Failure(val errors: List<String>) : ValidationResult
}

enum class PromptType {
    ANALYZE_IMAGE,
    TUTOR,
    GENERATE_EXERCISE,
    GRADE_EXERCISE,
}

data class AnalyzeImageRequest(
    val imageBytes: ByteArray,
    val mimeType: String = "image/jpeg",
    val gradeInstruction: String = "",
    val knowledgePointsList: String = "",
    val languageInstruction: String = "",
    val providerHints: String = "",
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnalyzeImageRequest) return false
        return imageBytes.contentEquals(other.imageBytes) &&
            mimeType == other.mimeType &&
            gradeInstruction == other.gradeInstruction &&
            knowledgePointsList == other.knowledgePointsList &&
            languageInstruction == other.languageInstruction &&
            providerHints == other.providerHints
    }

    override fun hashCode(): Int = imageBytes.contentHashCode()
}

data class AnalyzedQuestion(
    val subject: String,
    val knowledgePoints: List<String>,
    val requiresImage: Boolean,
    val wrongAnswerText: String,
    val mistakeStatus: MistakeStatus,
    val mistakeAnalysis: String,
    val questionText: String,
    val answerText: String,
    val analysis: String,
)

data class TutorRequest(
    val sessionId: TutorSessionId,
    val userMessage: String,
    val questionContext: String,
    val conversationContext: String,
    val gradeInstruction: String = "",
)

sealed interface AiStreamEvent {
    data class Delta(val text: String) : AiStreamEvent

    data class Usage(val inputTokens: Long?, val outputTokens: Long?) : AiStreamEvent

    data object Completed : AiStreamEvent
}

data class ExerciseRequest(
    val sessionId: TutorSessionId,
    val originalQuestion: String,
    val knowledgePoints: String,
    val difficulty: ExerciseDifficulty,
    val gradeInstruction: String = "",
    val sourceErrorItemId: ErrorItemId? = null,
    val subject: String = "",
)

data class GeneratedExercise(
    val questionText: String,
    val answerText: String,
    val analysis: String,
)

data class GradeExerciseRequest(
    val exerciseId: ExerciseId,
    val exerciseQuestion: String,
    val expectedAnswer: String,
    val userAnswer: String,
    val rubricContext: String = "",
)

data class ExerciseGrade(
    val result: GradeResult,
    val feedback: String,
    val confidence: Double,
)
