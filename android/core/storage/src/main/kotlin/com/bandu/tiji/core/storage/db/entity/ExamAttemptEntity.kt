package com.bandu.tiji.core.storage.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exam_attempts",
    foreignKeys = [
        ForeignKey(
            entity = ExamSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = BankQuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["question_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["session_id", "order_index"], unique = true),
        Index(value = ["question_id"]),
    ],
)
data class ExamAttemptEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    @ColumnInfo(name = "question_id")
    val questionId: String,
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
)
