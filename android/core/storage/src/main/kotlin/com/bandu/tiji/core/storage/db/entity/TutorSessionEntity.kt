package com.bandu.tiji.core.storage.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tutor_sessions",
    foreignKeys = [
        ForeignKey(
            entity = ErrorItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["error_item_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["error_item_id"]),
        Index(value = ["updated_at"]),
    ],
)
data class TutorSessionEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    @ColumnInfo(name = "error_item_id")
    val errorItemId: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
