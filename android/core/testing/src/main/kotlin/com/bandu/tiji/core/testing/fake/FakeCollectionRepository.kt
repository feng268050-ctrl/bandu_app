package com.bandu.tiji.core.testing.fake

import com.bandu.tiji.core.model.collection.CollectionSummary
import com.bandu.tiji.core.model.id.CollectionId
import com.bandu.tiji.core.testing.fixture.DEFAULT_FIXTURE_EPOCH_MILLIS
import com.bandu.tiji.domain.repository.CollectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeCollectionRepository(
    initialCollections: List<CollectionSummary> = emptyList(),
) : CollectionRepository {
    private val collections = MutableStateFlow(initialCollections.toList())
    private var nextId = initialCollections.size + 1

    val failures = FailureInjector()
    val createdNames = mutableListOf<String>()
    val renamedCollections = mutableListOf<Pair<CollectionId, String>>()
    val deletedIds = mutableListOf<CollectionId>()

    override fun observeCollections(): Flow<List<CollectionSummary>> = collections.asStateFlow()

    override suspend fun create(name: String): CollectionId {
        failures.throwIfQueued()
        val id = CollectionId("collection-${nextId++}")
        createdNames += name
        collections.value +=
            CollectionSummary(
                id = id,
                name = name,
                errorItemCount = 0,
                updatedAtEpochMillis = DEFAULT_FIXTURE_EPOCH_MILLIS,
            )
        return id
    }

    override suspend fun rename(id: CollectionId, name: String) {
        failures.throwIfQueued()
        renamedCollections += id to name
        collections.value =
            collections.value.map { collection ->
                if (collection.id == id) collection.copy(name = name) else collection
            }
    }

    override suspend fun delete(id: CollectionId) {
        failures.throwIfQueued()
        deletedIds += id
        collections.value = collections.value.filterNot { it.id == id }
    }

    fun emit(items: List<CollectionSummary>) {
        collections.value = items.toList()
    }
}
