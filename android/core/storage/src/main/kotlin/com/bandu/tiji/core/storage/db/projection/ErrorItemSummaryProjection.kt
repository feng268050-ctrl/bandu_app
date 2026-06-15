package com.bandu.tiji.core.storage.db.projection

import androidx.room.ColumnInfo

data class ErrorItemSummaryProjection(
    val id: String,
    @ColumnInfo(name = "collection_id")
    val collectionId: String,
    @ColumnInfo(name = "collection_name")
    val collectionName: String,
    @ColumnInfo(name = "image_path")
    val imagePath: String?,
    @ColumnInfo(name = "question_text")
    val questionText: String,
    @ColumnInfo(name = "mastery_level")
    val masteryLevel: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
