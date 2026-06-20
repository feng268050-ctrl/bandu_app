package com.bandu.tiji.feature.library

import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeErrorItemRepository
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ErrorItemBulkDeleteTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `bulk delete sends only selected ids and clears selection`() = runTest {
        val repository = FakeErrorItemRepository()
        val viewModel = ErrorItemListViewModel(repository)
        val first = ErrorItemId("first")
        val second = ErrorItemId("second")
        val unselected = ErrorItemId("unselected")

        viewModel.onAction(ErrorItemListAction.SelectAllLoaded(setOf(first, second, unselected)))
        viewModel.onAction(ErrorItemListAction.ToggleSelection(unselected))
        viewModel.onAction(ErrorItemListAction.RequestBulkDelete)

        assertThat(viewModel.uiState.value.bulkDelete?.ids)
            .containsExactly(first, second)

        viewModel.onAction(ErrorItemListAction.ConfirmBulkDelete)
        advanceUntilIdle()

        assertThat(repository.deletedIdSets).containsExactly(setOf(first, second))
        assertThat(viewModel.uiState.value.selectedIds).isEmpty()
        assertThat(viewModel.uiState.value.bulkDelete).isNull()
    }

    @Test
    fun `failed delete keeps exact count for retry`() = runTest {
        val repository = FakeErrorItemRepository().apply {
            failures.enqueue(IOException("disk"))
        }
        val viewModel = ErrorItemListViewModel(repository)
        viewModel.onAction(
            ErrorItemListAction.SelectAllLoaded(
                setOf(ErrorItemId("one"), ErrorItemId("two")),
            ),
        )
        viewModel.onAction(ErrorItemListAction.RequestBulkDelete)
        viewModel.onAction(ErrorItemListAction.ConfirmBulkDelete)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.bulkDelete?.ids).hasSize(2)
        assertThat(viewModel.uiState.value.bulkDelete?.errorMessage)
            .isEqualTo("无法删除选中的错题")
    }
}
