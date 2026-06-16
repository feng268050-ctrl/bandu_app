package com.bandu.tiji.core.storage.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "error_items",
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collection_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["updated_at", "id"]),
        Index(value = ["collection_id", "updated_at"]),
        Index(value = ["mastery_level", "updated_at"]),
        Index(value = ["grade_semester", "paper_level"]),
        Index(value = ["created_at"]),
    ],
)
data class ErrorItemEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "collection_id")
    val collectionId: String,
    @ColumnInfo(name = "image_path")
    val imagePath: String?,
    @ColumnInfo(name = "image_sha256")
    val imageSha256: String?,
    @ColumnInfo(name = "image_width")
    val imageWidth: Int?,
    @ColumnInfo(name = "image_height")
    val imageHeight: Int?,
    @ColumnInfo(name = "question_text")
    val questionText: String,
    @ColumnInfo(name = "answer_text")
    val answerText: String,
    val analysis: String,
    @ColumnInfo(name = "wrong_answer_text", defaultValue = "''")
    val wrongAnswerText: String = "",
    @ColumnInfo(name = "mistake_status")
    val mistakeStatus: String,
    @ColumnInfo(name = "mistake_analysis", defaultValue = "''")
    val mistakeAnalysis: String = "",
    val subject: String,
    @ColumnInfo(name = "grade_semester")
    val gradeSemester: String?,
    @ColumnInfo(name = "paper_level")
    val paperLevel: String?,
    @ColumnInfo(defaultValue = "''")
    val notes: String = "",
    @ColumnInfo(name = "mastery_level")
    val masteryLevel: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
