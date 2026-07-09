package com.bandu.tiji.core.storage.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "question_banks",
    indices = [
        Index(value = ["updated_at"]),
        Index(value = ["name"]),
    ],
)
data class QuestionBankEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val name: String,
    @ColumnInfo(name = "source_type")
    val sourceType: String,
    @ColumnInfo(name = "source_file_name")
    val sourceFileName: String,
    @ColumnInfo(name = "source_uri")
    val sourceUri: String,
    val subject: String?,
    @ColumnInfo(name = "import_status")
    val importStatus: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
