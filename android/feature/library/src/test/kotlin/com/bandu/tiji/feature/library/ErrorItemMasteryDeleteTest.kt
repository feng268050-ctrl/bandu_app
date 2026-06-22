package com.bandu.tiji.feature.library

import com.bandu.tiji.core.model.enums.MasteryLevel
import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeErrorItemRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ErrorItemMasteryDeleteTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `mastery action updates each of the three states`() = runTest {
        val item = detailItem()
        val repository = FakeErrorItemRepository(listOf(item))
        val viewModel = ErrorItemDetailViewModel(repository, item.id)
        advanceUntilIdle()

        listOf(MasteryLevel.NEW, MasteryLevel.REVIEWING, MasteryLevel.MASTERED).forEach { level ->
            if (viewModel.uiState.value.item?.masteryLevel != level) {
                viewModel.onAction(ErrorItemDetailAction.UpdateMastery(level))
                advanceUntilIdle()
            }
        }

        assertThat(repository.updatedItems.mapNotNull { it.second.masteryLevel })
            .containsExactly(MasteryLevel.NEW, MasteryLevel.REVIEWING, MasteryLevel.MASTERED)
            .inOrder()
        assertThat(viewModel.uiState.value.item?.masteryLevel).isEqualTo(MasteryLevel.MASTERED)
    }

    @Test
    fun `confirmed single delete removes requested item and navigates back`() = runTest {
        val item = detailItem()
        val repository = FakeErrorItemRepository(listOf(item))
        val viewModel = ErrorItemDetailViewModel(repository, item.id)
        advanceUntilIdle()

        viewModel.onAction(ErrorItemDetailAction.RequestDelete)
        assertThat(viewModel.uiState.value.pendingDelete).isNotNull()
        val effect = async { viewModel.effects.first() }
        viewModel.onAction(ErrorItemDetailAction.ConfirmDelete)
        advanceUntilIdle()

        assertThat(repository.deletedIdSets).containsExactly(setOf(item.id))
        assertThat(effect.await()).isEqualTo(ErrorItemDetailEffect.NavigateBack)
        assertThat(viewModel.uiState.value.pendingDelete).isNull()
    }

    @Test
    fun `dismissed delete leaves item untouched`() = runTest {
        val item = detailItem()
        val repository = FakeErrorItemRepository(listOf(item))
        val viewModel = ErrorItemDetailViewModel(repository, item.id)
        advanceUntilIdle()

        viewModel.onAction(ErrorItemDetailAction.RequestDelete)
        viewModel.onAction(ErrorItemDetailAction.DismissDelete)
        advanceUntilIdle()

        assertThat(repository.deletedIdSets).isEmpty()
        assertThat(viewModel.uiState.value.pendingDelete).isNull()
        assertThat(viewModel.uiState.value.item).isEqualTo(item)
    }

    @Test
    fun `repository failures keep controls retryable`() = runTest {
        val item = detailItem()
        val repository = FakeErrorItemRepository(listOf(item))
        val viewModel = ErrorItemDetailViewModel(repository, item.id)
        advanceUntilIdle()

        repository.failures.enqueue(IllegalStateException("disk"))
        viewModel.onAction(ErrorItemDetailAction.UpdateMastery(MasteryLevel.MASTERED))
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.masteryErrorMessage)
            .isEqualTo("无法更新掌握状态")
        assertThat(viewModel.uiState.value.isUpdatingMastery).isFalse()

        repository.failures.enqueue(IllegalStateException("disk"))
        viewModel.onAction(ErrorItemDetailAction.RequestDelete)
        viewModel.onAction(ErrorItemDetailAction.ConfirmDelete)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.pendingDelete?.errorMessage)
            .isEqualTo("无法删除错题")
        assertThat(viewModel.uiState.value.pendingDelete?.isDeleting).isFalse()
    }
}
