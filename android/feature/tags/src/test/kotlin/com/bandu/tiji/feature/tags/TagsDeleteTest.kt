package com.bandu.tiji.feature.tags

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeTagRepository
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
class TagsDeleteViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `delete confirmation keeps linked count and deletes only custom tag`() = runTest {
        val custom = tagNode(
            id = "custom",
            name = "易错公式",
            subject = "数学",
            isSystem = false,
            count = 12,
        )
        val repository = FakeTagRepository(listOf(custom))
        val viewModel = TagsViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(TagsAction.OpenDelete(custom))

        assertThat(viewModel.uiState.value.deleteConfirmation).isEqualTo(
            TagDeleteConfirmation(
                tagId = custom.tag.id,
                name = "易错公式",
                linkedErrorItemCount = 12,
            ),
        )

        viewModel.onAction(TagsAction.ConfirmDelete)
        advanceUntilIdle()

        assertThat(repository.deletedIds).containsExactly(custom.tag.id)
        assertThat(viewModel.uiState.value.deleteConfirmation).isNull()
    }

    @Test
    fun `standard tag cannot open delete confirmation`() = runTest {
        val standard = tagNode("standard", "函数", "数学", isSystem = true)
        val viewModel = TagsViewModel(FakeTagRepository(listOf(standard)))
        advanceUntilIdle()

        viewModel.onAction(TagsAction.OpenDelete(standard))

        assertThat(viewModel.uiState.value.deleteConfirmation).isNull()
    }

    @Test
    fun `delete failure keeps confirmation for retry`() = runTest {
        val custom = tagNode("custom", "函数", "数学", isSystem = false)
        val repository = FakeTagRepository(listOf(custom)).apply {
            failures.enqueue(IOException("disk unavailable"))
        }
        val viewModel = TagsViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(TagsAction.OpenDelete(custom))
        viewModel.onAction(TagsAction.ConfirmDelete)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.deleteConfirmation?.errorMessage)
            .isEqualTo("无法删除标签")
        assertThat(viewModel.uiState.value.deleteConfirmation?.isSubmitting).isFalse()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TagsDeleteDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `dialog explains linked count and preserves wrong items`() {
        val actions = mutableListOf<TagsAction>()
        composeRule.setContent {
            BanduTijiTheme {
                TagsScreen(
                    uiState = TagsUiState(
                        isLoading = false,
                        deleteConfirmation = TagDeleteConfirmation(
                            tagId = com.bandu.tiji.core.model.id.TagId("custom"),
                            name = "易错公式",
                            linkedErrorItemCount = 12,
                        ),
                    ),
                    onAction = actions::add,
                )
            }
        }

        composeRule.onNodeWithText("“易错公式”关联 12 道错题。删除标签不会删除这些错题。")
            .assertIsDisplayed()
        composeRule.onNodeWithText("确认删除").performClick()
        composeRule.runOnIdle {
            assertThat(actions).contains(TagsAction.ConfirmDelete)
        }
    }
}
