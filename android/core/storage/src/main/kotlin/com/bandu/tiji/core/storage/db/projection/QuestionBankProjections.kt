package com.bandu.tiji.core.storage.db.projection

import androidx.room.ColumnInfo

data class QuestionBankSummaryProjection(
    val id: String,
    val name: String,
    @ColumnInfo(name = "source_file_name")
    val sourceFileName: String,
    @ColumnInfo(name = "source_uri")
    val sourceUri: String,
    val subject: String?,
    @ColumnInfo(name = "import_status")
    val importStatus: String,
    @ColumnInfo(name = "question_count")
    val questionCount: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

data class ExamAttemptQuestionProjection(
    @ColumnInfo(name = "attempt_id")
    val attemptId: String,
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    @ColumnInfo(name = "order_index")
    val orderIndex: Int,
    @ColumnInfo(name = "user_answer")
    val userAnswer: String?,
    @ColumnInfo(name = "answer_revealed")
    val answerRevealed: Boolean,
    @ColumnInfo(name = "grading_result")
    val gradingResult: String,
    @ColumnInfo(name = "grading_source")
    val gradingSource: String,
    @ColumnInfo(name = "grading_feedback")
    val gradingFeedback: String?,
    @ColumnInfo(name = "submitted_at")
    val submittedAt: Long?,
    @ColumnInfo(name = "question_id")
    val questionId: String,
    @ColumnInfo(name = "bank_id")
    val bankId: String,
    val stem: String,
    @ColumnInfo(name = "options_text")
    val optionsText: String,
    val answer: String?,
    val analysis: String?,
    @ColumnInfo(name = "question_type")
    val questionType: String,
    val difficulty: String,
    @ColumnInfo(name = "tags_text")
    val tagsText: String,
    @ColumnInfo(name = "source_page")
    val sourcePage: Int?,
    @ColumnInfo(name = "source_text")
    val sourceText: String?,
    @ColumnInfo(name = "source_image_uri")
    val sourceImageUri: String?,
    @ColumnInfo(name = "review_status")
    val reviewStatus: String,
    @ColumnInfo(name = "question_created_at")
    val questionCreatedAt: Long,
    @ColumnInfo(name = "question_updated_at")
    val questionUpdatedAt: Long,
)
