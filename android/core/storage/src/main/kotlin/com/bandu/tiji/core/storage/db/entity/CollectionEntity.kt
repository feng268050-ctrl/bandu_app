package com.bandu.tiji.core.storage.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "collections",
    indices = [
        Index(value = ["name"], unique = true),
    ],
)
data class CollectionEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val name: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
