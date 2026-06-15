package com.bandu.tiji.core.storage.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "error_item_tags",
    primaryKeys = ["error_item_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = ErrorItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["error_item_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["tag_id", "error_item_id"]),
    ],
)
data class ErrorItemTagEntity(
    @ColumnInfo(name = "error_item_id")
    val errorItemId: String,
    @ColumnInfo(name = "tag_id")
    val tagId: String,
)
