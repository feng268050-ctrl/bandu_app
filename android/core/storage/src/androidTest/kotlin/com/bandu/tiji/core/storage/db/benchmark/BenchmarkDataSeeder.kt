package com.bandu.tiji.core.storage.db.benchmark

import androidx.room.withTransaction
import com.bandu.tiji.core.storage.db.LearningDatabase
import com.bandu.tiji.core.storage.db.entity.CollectionEntity
import com.bandu.tiji.core.storage.db.entity.ErrorItemEntity
import com.bandu.tiji.core.storage.db.entity.ErrorItemTagEntity
import com.bandu.tiji.core.storage.db.entity.TagEntity

internal class BenchmarkDataSeeder(
    private val database: LearningDatabase,
) {
    suspend fun seed(itemCount: Int = DEFAULT_ITEM_COUNT) {
        database.withTransaction {
            repeat(COLLECTION_COUNT) { index ->
                database.collectionDao().insert(
                    CollectionEntity(
                        id = collectionId(index),
                        name = "题集 ${index + 1}",
                        createdAt = 1,
                        updatedAt = 1,
                    ),
                )
            }
            repeat(TAG_COUNT) { index ->
                database.tagDao().insert(
                    TagEntity(
                        id = tagId(index),
                        name = "知识点 ${index + 1}",
                        subject = if (index % 2 == 0) "数学" else "物理",
                        parentId = null,
                        sortOrder = index,
                        code = "benchmark-$index",
                        isSystem = false,
                        createdAt = 1,
                        updatedAt = 1,
                    ),
                )
            }
            repeat(itemCount) { index ->
                val collectionIndex = index % COLLECTION_COUNT
                val questionPrefix = if (index % 5 == 0) "quadratic" else "linear"
                database.errorItemDao().insert(
                    ErrorItemEntity(
                        id = itemId(index),
                        collectionId = collectionId(collectionIndex),
                        imagePath = "images/${itemId(index)}.jpg",
                        imageSha256 = "sha256-${itemId(index)}",
                        imageWidth = 1280,
                        imageHeight = 720,
                        questionText = "$questionPrefix benchmark question $index",
                        answerText = "answer $index",
                        analysis = "analysis $index",
                        wrongAnswerText = "wrong $index",
                        mistakeStatus = if (index % 3 == 0) "CONFIRMED" else "UNKNOWN",
                        mistakeAnalysis = "mistake $index",
                        subject = if (collectionIndex % 2 == 0) "数学" else "物理",
                        gradeSemester = GRADE_SEMESTERS[index % GRADE_SEMESTERS.size],
                        paperLevel = PAPER_LEVELS[index % PAPER_LEVELS.size],
                        notes = "notes $index",
                        masteryLevel = index % 4,
                        createdAt = index.toLong(),
                        updatedAt = index.toLong(),
                    ),
                )
                database.tagDao().link(
                    ErrorItemTagEntity(
                        errorItemId = itemId(index),
                        tagId = tagId(index % TAG_COUNT),
                    ),
                )
                if (index % 3 == 0) {
                    database.tagDao().link(
                        ErrorItemTagEntity(
                            errorItemId = itemId(index),
                            tagId = tagId((index + 1) % TAG_COUNT),
                        ),
                    )
                }
            }
        }
    }

    companion object {
        const val DEFAULT_ITEM_COUNT = 5_000
        private const val COLLECTION_COUNT = 10
        private const val TAG_COUNT = 20
        private val GRADE_SEMESTERS = listOf("高一上", "高一下", "高二上", "高二下")
        private val PAPER_LEVELS = listOf("A", "B", "C")

        fun itemId(index: Int): String = "benchmark-${index.toString().padStart(5, '0')}"

        fun collectionId(index: Int): String = "collection-${index.toString().padStart(2, '0')}"

        fun tagId(index: Int): String = "tag-${index.toString().padStart(2, '0')}"
    }
}
