package com.bandu.tiji.core.storage.tags

import androidx.room.withTransaction
import com.bandu.tiji.core.storage.db.LearningDatabase

class StandardTagSeeder(
    private val database: LearningDatabase,
    private val assetReader: StandardTagAssetReader,
) {
    suspend fun seed(nowEpochMillis: Long) {
        val tags = assetReader.read().map { it.toEntity(nowEpochMillis) }
        database.withTransaction {
            database.tagDao().upsertSystemTags(tags)
        }
    }
}
