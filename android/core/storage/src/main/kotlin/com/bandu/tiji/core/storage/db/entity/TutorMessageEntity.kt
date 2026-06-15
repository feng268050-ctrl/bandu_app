package com.bandu.tiji.core.storage.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tutor_messages",
    foreignKeys = [
        ForeignKey(
            entity = TutorSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["session_id", "sequence"], unique = true),
    ],
)
data class TutorMessageEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    val role: String,
    val content: String,
    val status: String,
    val sequence: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
