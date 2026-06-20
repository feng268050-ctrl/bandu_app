package com.bandu.tiji.feature.library

import app.cash.turbine.test
import com.bandu.tiji.core.model.collection.CollectionSummary
import com.bandu.tiji.core.model.id.CollectionId
import com.bandu.tiji.core.model.navigation.NavigationIntent
import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeCollectionRepository
import com.bandu.tiji.domain.repository.CollectionRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `collections are sorted by most recently updated`() = runTest {
        val older = collection("older", 10L)
        val newer = collection("newer", 20L)
        val viewModel = CollectionListViewModel(
            FakeCollectionRepository(listOf(older, newer)),
        )

        advanceUntilIdle()

        assertThat(viewModel.uiState.value.collections).containsExactly(newer, older).inOrder()
        assertThat(viewModel.uiState.value.isLoading).isFalse()
    }

    @Test
    fun `repository failure is recoverable with retry`() = runTest {
        val repository = RetryCollectionRepository()
        val viewModel = CollectionListViewModel(repository)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("无法加载题集")

        viewModel.onAction(CollectionListAction.Retry)
        advanceUntilIdle()

        assertThat(repository.observationCount).isEqualTo(2)
        assertThat(viewModel.uiState.value.errorMessage).isNull()
        assertThat(viewModel.uiState.value.collections).hasSize(1)
    }

    @Test
    fun `opening collection emits domain navigation intent`() = runTest {
        val viewModel = CollectionListViewModel(FakeCollectionRepository())

        viewModel.effects.test {
            viewModel.onAction(
                CollectionListAction.OpenCollection(CollectionId("collection-9")),
            )
            assertThat(awaitItem()).isEqualTo(
                CollectionListEffect.Navigate(
                    NavigationIntent.OpenCollection("collection-9"),
                ),
            )
        }
    }

    private fun collection(id: String, updatedAt: Long) =
        CollectionSummary(
            id = CollectionId(id),
            name = id,
            errorItemCount = 0,
            updatedAtEpochMillis = updatedAt,
        )

    private class RetryCollectionRepository : CollectionRepository {
        var observationCount = 0

        override fun observeCollections(): Flow<List<CollectionSummary>> {
            observationCount += 1
            return if (observationCount == 1) {
                flow { throw IllegalStateException("failed") }
            } else {
                flow { emit(listOf(collection("recovered", 1L))) }
            }
        }

        override suspend fun create(name: String): CollectionId =
            error("Not used")

        override suspend fun rename(id: CollectionId, name: String) {
            error("Not used")
        }

        override suspend fun delete(id: CollectionId) {
            error("Not used")
        }

        private fun collection(id: String, updatedAt: Long) =
            CollectionSummary(
                id = CollectionId(id),
                name = id,
                errorItemCount = 0,
                updatedAtEpochMillis = updatedAt,
            )
    }
}
