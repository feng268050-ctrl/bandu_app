package com.bandu.tiji.data.repository

import androidx.room.withTransaction
import com.bandu.tiji.core.common.id.UuidGenerator
import com.bandu.tiji.core.common.time.Clock
import com.bandu.tiji.core.model.collection.CollectionSummary
import com.bandu.tiji.core.model.id.CollectionId
import com.bandu.tiji.core.storage.db.LearningDatabase
import com.bandu.tiji.core.storage.db.entity.CollectionEntity
import com.bandu.tiji.data.mapper.toDomainSummary
import com.bandu.tiji.domain.repository.CollectionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RoomCollectionRepository @Inject constructor(
    private val database: LearningDatabase,
    private val uuidGenerator: UuidGenerator,
    private val clock: Clock,
) : CollectionRepository {
    private val collectionDao = database.collectionDao()

    override fun observeCollections(): Flow<List<CollectionSummary>> =
        collectionDao.observeAll().map { collections ->
            collections.map { entity ->
                entity.toDomainSummary(errorItemCount = database.countErrorItems(entity.id))
            }
        }

    override suspend fun create(name: String): CollectionId {
        val now = clock.nowEpochMillis()
        val id = CollectionId(uuidGenerator.newUuid())
        collectionDao.insert(
            CollectionEntity(
                id = id.value,
                name = name,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return id
    }

    override suspend fun rename(id: CollectionId, name: String) {
        val now = clock.nowEpochMillis()
        database.withTransaction {
            val current = collectionDao.getById(id.value) ?: return@withTransaction
            collectionDao.update(current.copy(name = name, updatedAt = now))
        }
    }

    override suspend fun delete(id: CollectionId) {
        database.withTransaction {
            collectionDao.getById(id.value)?.let { collection ->
                collectionDao.delete(collection)
            }
        }
    }

    private fun LearningDatabase.countErrorItems(collectionId: String): Int =
        openHelper.readableDatabase
            .query(
                "SELECT COUNT(*) FROM error_items WHERE collection_id = ?",
                arrayOf(collectionId),
            )
            .use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
}
