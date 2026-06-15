package com.bandu.tiji.core.storage.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = ErrorItemEntity::class)
@Entity(tableName = "error_item_fts")
data class ErrorItemFtsEntity(
    @ColumnInfo(name = "question_text")
    val questionText: String,
    @ColumnInfo(name = "answer_text")
    val answerText: String,
    val analysis: String,
    @ColumnInfo(name = "wrong_answer_text")
    val wrongAnswerText: String,
    @ColumnInfo(name = "mistake_analysis")
    val mistakeAnalysis: String,
    val notes: String,
)
