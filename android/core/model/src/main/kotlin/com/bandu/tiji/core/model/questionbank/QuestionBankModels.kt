package com.bandu.tiji.core.model.questionbank

import com.bandu.tiji.core.model.enums.ExerciseDifficulty
import com.bandu.tiji.core.model.id.BankQuestionId
import com.bandu.tiji.core.model.id.ExamAttemptId
import com.bandu.tiji.core.model.id.ExamSessionId
import com.bandu.tiji.core.model.id.QuestionBankId

data class QuestionBankSummary(
    val id: QuestionBankId,
    val name: String,
    val sourceFileName: String,
    val sourceUri: String,
    val subject: String?,
    val questionCount: Int,
    val importStatus: QuestionBankImportStatus,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

data class QuestionBank(
    val id: QuestionBankId,
    val name: String,
    val sourceFileName: String,
    val sourceUri: String,
    val subject: String?,
    val importStatus: QuestionBankImportStatus,
    val questions: List<BankQuestion>,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

data class QuestionBankDraft(
    val name: String,
    val sourceFileName: String,
    val sourceUri: String,
    val subject: String? = null,
) {
    init {
        require(name.isNotBlank()) { "name must not be blank" }
        require(sourceFileName.isNotBlank()) { "sourceFileName must not be blank" }
        require(sourceUri.isNotBlank()) { "sourceUri must not be blank" }
    }
}

data class BankQuestion(
    val id: BankQuestionId,
    val bankId: QuestionBankId,
    val stem: String,
    val options: List<String>,
    val answer: String?,
    val analysis: String?,
    val questionType: BankQuestionType,
    val difficulty: ExerciseDifficulty,
    val tags: List<String>,
    val sourcePage: Int?,
    val sourceText: String?,
    val sourceImageUri: String?,
    val reviewStatus: QuestionReviewStatus,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

data class BankQuestionDraft(
    val bankId: QuestionBankId,
    val stem: String,
    val options: List<String> = emptyList(),
    val answer: String? = null,
    val analysis: String? = null,
    val questionType: BankQuestionType = BankQuestionType.UNKNOWN,
    val difficulty: ExerciseDifficulty = ExerciseDifficulty.MEDIUM,
    val tags: List<String> = emptyList(),
    val sourcePage: Int? = null,
    val sourceText: String? = null,
    val sourceImageUri: String? = null,
    val reviewStatus: QuestionReviewStatus = QuestionReviewStatus.USER_CONFIRMED,
) {
    init {
        require(stem.isNotBlank()) { "stem must not be blank" }
        require(sourcePage == null || sourcePage > 0) { "sourcePage must be positive" }
    }
}

data class ExamSessionSummary(
    val id: ExamSessionId,
    val bankId: QuestionBankId,
    val title: String,
    val questionCount: Int,
    val status: ExamSessionStatus,
    val createdAtEpochMillis: Long,
    val completedAtEpochMillis: Long?,
)

data class ExamSession(
    val id: ExamSessionId,
    val bankId: QuestionBankId,
    val title: String,
    val seed: Long,
    val status: ExamSessionStatus,
    val attempts: List<ExamAttempt>,
    val createdAtEpochMillis: Long,
    val completedAtEpochMillis: Long?,
)

data class ExamAttempt(
    val id: ExamAttemptId,
    val sessionId: ExamSessionId,
    val question: BankQuestion,
    val orderIndex: Int,
    val userAnswer: String?,
    val answerRevealed: Boolean,
    val gradingResult: ExamGradingResult,
    val gradingSource: ExamGradingSource,
    val gradingFeedback: String?,
    val submittedAtEpochMillis: Long?,
)

enum class QuestionBankImportStatus {
    PENDING,
    REVIEW_REQUIRED,
    IMPORTED,
    FAILED,
}

enum class BankQuestionType {
    SINGLE_CHOICE,
    MULTIPLE_CHOICE,
    FILL_BLANK,
    SUBJECTIVE,
    UNKNOWN,
}

enum class QuestionReviewStatus {
    AUTO_CONFIRMED,
    NEEDS_REVIEW,
    USER_CONFIRMED,
}

enum class ExamSessionStatus {
    IN_PROGRESS,
    COMPLETED,
}

enum class ExamGradingResult {
    CORRECT,
    INCORRECT,
    NEEDS_REVIEW,
    NOT_GRADED,
}

enum class ExamGradingSource {
    LOCAL,
    AI,
    MANUAL,
    NONE,
}
