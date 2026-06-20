package com.bandu.tiji.feature.library

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
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
class CollectionEditorViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `create and rename persist trimmed names`() = runTest {
        val existing = collection("one", "旧名称")
        val repository = FakeCollectionRepository(listOf(existing))
        val viewModel = CollectionListViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(CollectionListAction.RequestCreate)
        viewModel.onAction(CollectionListAction.UpdateEditorName("  新题集  "))
        viewModel.onAction(CollectionListAction.SubmitEditor)
        advanceUntilIdle()

        viewModel.onAction(CollectionListAction.RequestRename(existing.id))
        viewModel.onAction(CollectionListAction.UpdateEditorName("  新名称  "))
        viewModel.onAction(CollectionListAction.SubmitEditor)
        advanceUntilIdle()

        assertThat(repository.createdNames).containsExactly("新题集")
        assertThat(repository.renamedCollections).containsExactly(existing.id to "新名称")
        assertThat(viewModel.uiState.value.editor).isNull()
    }

    @Test
    fun `blank and duplicate names stay in editor with specific errors`() = runTest {
        val existing = collection("one", "数学")
        val repository = FakeCollectionRepository(listOf(existing))
        val viewModel = CollectionListViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(CollectionListAction.RequestCreate)
        viewModel.onAction(CollectionListAction.UpdateEditorName(" "))
        viewModel.onAction(CollectionListAction.SubmitEditor)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.editor?.errorMessage).isEqualTo("题集名称不能为空")

        viewModel.onAction(CollectionListAction.UpdateEditorName(" 数学 "))
        viewModel.onAction(CollectionListAction.SubmitEditor)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.editor?.errorMessage).isEqualTo("题集名称已存在")
        assertThat(repository.createdNames).isEmpty()
    }

    @Test
    fun `repository failure is shown and editor input is preserved`() = runTest {
        val repository = FakeCollectionRepository()
        repository.failures.enqueue(IllegalStateException("disk"))
        val viewModel = CollectionListViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(CollectionListAction.RequestCreate)
        viewModel.onAction(CollectionListAction.UpdateEditorName("保留输入"))
        viewModel.onAction(CollectionListAction.SubmitEditor)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.editor?.name).isEqualTo("保留输入")
        assertThat(viewModel.uiState.value.editor?.errorMessage).isEqualTo("无法保存题集")
        assertThat(viewModel.uiState.value.editor?.isSaving).isFalse()
    }

    @Test
    fun `dismissing editor clears unsaved input`() = runTest {
        val viewModel = CollectionListViewModel(FakeCollectionRepository())
        advanceUntilIdle()

        viewModel.onAction(CollectionListAction.RequestCreate)
        viewModel.onAction(CollectionListAction.UpdateEditorName("不保存"))
        viewModel.onAction(CollectionListAction.DismissEditor)

        assertThat(viewModel.uiState.value.editor).isNull()
    }

    private fun collection(id: String, name: String) =
        CollectionSummary(
            id = CollectionId(id),
            name = name,
            errorItemCount = 0,
            updatedAtEpochMillis = 0L,
        )
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CollectionEditorScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `create dialog edits name and submits`() {
        val actions = mutableListOf<CollectionListAction>()
        composeRule.setContent {
            BanduTijiTheme {
                CollectionListScreen(
                    uiState = CollectionListUiState(
                        isLoading = false,
                        editor = CollectionEditorUiState(
                            mode = CollectionEditorMode.Create,
                            name = "",
                        ),
                    ),
                    onAction = actions::add,
                )
            }
        }

        composeRule.onNodeWithText("题集名称").performTextReplacement("新题集")
        composeRule.onNodeWithText("保存").performClick()

        composeRule.runOnIdle {
            assertThat(actions).containsExactly(
                CollectionListAction.UpdateEditorName("新题集"),
                CollectionListAction.SubmitEditor,
            ).inOrder()
        }
    }

    @Test
    fun `editor displays validation error`() {
        composeRule.setContent {
            BanduTijiTheme {
                CollectionListScreen(
                    uiState = CollectionListUiState(
                        isLoading = false,
                        editor = CollectionEditorUiState(
                            mode = CollectionEditorMode.Create,
                            name = "",
                            errorMessage = "题集名称不能为空",
                        ),
                    ),
                    onAction = {},
                )
            }
        }

        composeRule.onNodeWithText("题集名称不能为空").assertIsDisplayed()
    }
}
