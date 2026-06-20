package com.bandu.tiji.feature.library

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import com.bandu.tiji.core.model.collection.CollectionSummary
import com.bandu.tiji.core.model.id.CollectionId
import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeCollectionRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionDeleteViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `delete runs only after explicit confirmation`() = runTest {
        val collection = collection()
        val repository = FakeCollectionRepository(listOf(collection))
        val viewModel = CollectionListViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(CollectionListAction.RequestDelete(collection.id))
        assertThat(repository.deletedIds).isEmpty()

        viewModel.onAction(CollectionListAction.ConfirmDelete)
        advanceUntilIdle()

        assertThat(repository.deletedIds).containsExactly(collection.id)
        assertThat(viewModel.uiState.value.pendingDelete).isNull()
    }

    @Test
    fun `dismiss and repository failure do not hide impact prematurely`() = runTest {
        val collection = collection()
        val repository = FakeCollectionRepository(listOf(collection))
        val viewModel = CollectionListViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(CollectionListAction.RequestDelete(collection.id))
        viewModel.onAction(CollectionListAction.DismissDelete)
        assertThat(repository.deletedIds).isEmpty()

        repository.failures.enqueue(IllegalStateException("foreign key"))
        viewModel.onAction(CollectionListAction.RequestDelete(collection.id))
        viewModel.onAction(CollectionListAction.ConfirmDelete)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.pendingDelete?.collection).isEqualTo(collection)
        assertThat(viewModel.uiState.value.pendingDelete?.errorMessage).isEqualTo("无法删除题集")
    }

    private fun collection() =
        CollectionSummary(
            id = CollectionId("collection-1"),
            name = "期中复习",
            errorItemCount = 8,
            updatedAtEpochMillis = 0L,
        )
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CollectionDeleteScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `non-empty collection confirmation displays affected item count`() {
        val actions = mutableListOf<CollectionListAction>()
        val collection = CollectionSummary(
            id = CollectionId("collection-1"),
            name = "期中复习",
            errorItemCount = 8,
            updatedAtEpochMillis = 0L,
        )
        composeRule.setContent {
            BanduTijiTheme {
                CollectionListScreen(
                    uiState = CollectionListUiState(
                        collections = listOf(collection),
                        isLoading = false,
                        pendingDelete = CollectionDeleteUiState(collection),
                    ),
                    onAction = actions::add,
                )
            }
        }

        composeRule.onNodeWithText("将同时永久删除其中的 8 道错题", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("确认删除").performClick()
        composeRule.runOnIdle {
            assertThat(actions).contains(CollectionListAction.ConfirmDelete)
        }
    }
}
