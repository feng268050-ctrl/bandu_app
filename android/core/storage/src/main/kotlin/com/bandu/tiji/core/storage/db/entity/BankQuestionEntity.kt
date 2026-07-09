package com.bandu.tiji.core.storage.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bank_questions",
    foreignKeys = [
        ForeignKey(
            entity = QuestionBankEntity::class,
            parentColumns = ["id"],
            childColumns = ["bank_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["bank_id", "created_at"]),
        Index(value = ["question_type"]),
        Index(value = ["difficulty"]),
    ],
)
data class BankQuestionEntity(
    @PrimaryKey
    val id: String,
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
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
