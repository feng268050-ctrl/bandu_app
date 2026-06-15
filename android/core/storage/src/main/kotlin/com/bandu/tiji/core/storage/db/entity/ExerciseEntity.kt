package com.bandu.tiji.core.storage.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercises",
    foreignKeys = [
        ForeignKey(
            entity = TutorSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ErrorItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_error_item_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["session_id"]),
        Index(value = ["source_error_item_id"]),
        Index(value = ["created_at"]),
    ],
)
data class ExerciseEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    @ColumnInfo(name = "source_error_item_id")
    val sourceErrorItemId: String?,
    val subject: String,
    val difficulty: String,
    @ColumnInfo(name = "question_text")
    val questionText: String,
    @ColumnInfo(name = "expected_answer")
    val expectedAnswer: String,
    val analysis: String,
    @ColumnInfo(name = "user_answer")
    val userAnswer: String?,
    @ColumnInfo(name = "ai_result")
    val aiResult: String?,
    @ColumnInfo(name = "final_result")
    val finalResult: String?,
    @ColumnInfo(name = "grading_feedback")
    val gradingFeedback: String?,
    @ColumnInfo(name = "graded_at")
    val gradedAt: Long?,
    @ColumnInfo(name = "overridden_at")
    val overriddenAt: Long?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
