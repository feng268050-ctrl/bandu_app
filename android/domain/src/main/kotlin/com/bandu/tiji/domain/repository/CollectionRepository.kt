package com.bandu.tiji.domain.repository

import com.bandu.tiji.core.model.collection.CollectionSummary
import com.bandu.tiji.core.model.id.CollectionId
import kotlinx.coroutines.flow.Flow

interface CollectionRepository {
    fun observeCollections(): Flow<List<CollectionSummary>>

    suspend fun create(name: String): CollectionId

    suspend fun rename(id: CollectionId, name: String)

    suspend fun delete(id: CollectionId)
}
