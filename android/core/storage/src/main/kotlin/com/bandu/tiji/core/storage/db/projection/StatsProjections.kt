package com.bandu.tiji.core.storage.db.projection

import androidx.room.ColumnInfo

data class WrongItemTotalsProjection(
    @ColumnInfo(name = "total_count")
    val totalCount: Int,
    @ColumnInfo(name = "mastered_count")
    val masteredCount: Int,
)

data class ExerciseTotalsProjection(
    @ColumnInfo(name = "total_count")
    val totalCount: Int,
    @ColumnInfo(name = "graded_count")
    val gradedCount: Int,
    @ColumnInfo(name = "correct_count")
    val correctCount: Int,
)

data class DimensionCountProjection(
    val dimension: String,
    val count: Int,
)
