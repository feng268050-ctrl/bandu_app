package com.bandu.tiji.feature.tags

import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeTagRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TagsRenameViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `rename editor is prefilled and successful name is trimmed`() = runTest {
        val custom = tagNode(
            id = "custom",
            name = "原名称",
            subject = "数学",
            isSystem = false,
        )
        val repository = FakeTagRepository(listOf(custom))
        val viewModel = TagsViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(TagsAction.OpenRename(custom))
        assertThat(viewModel.uiState.value.editor).isEqualTo(
            TagEditorState(
                mode = TagEditorMode.Rename(
                    tagId = custom.tag.id,
                    originalName = "原名称",
                ),
                name = "原名称",
            ),
        )

        viewModel.onAction(TagsAction.EditorNameChanged("  新名称  "))
        viewModel.onAction(TagsAction.SubmitEditor)
        advanceUntilIdle()

        assertThat(repository.renamedTags).containsExactly(custom.tag.id to "新名称")
        assertThat(viewModel.uiState.value.editor).isNull()
    }

    @Test
    fun `rename conflict stays in editor with duplicate message`() = runTest {
        val custom = tagNode(
            id = "custom",
            name = "原名称",
            subject = "数学",
            isSystem = false,
        )
        val repository = FakeTagRepository(listOf(custom)).apply {
            failures.enqueue(IllegalStateException("UNIQUE constraint failed: tags.name"))
        }
        val viewModel = TagsViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(TagsAction.OpenRename(custom))
        viewModel.onAction(TagsAction.EditorNameChanged("重复名称"))
        viewModel.onAction(TagsAction.SubmitEditor)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.editor?.errorMessage)
            .isEqualTo("同一位置已存在同名标签")
        assertThat(viewModel.uiState.value.editor?.isSubmitting).isFalse()
    }

    @Test
    fun `rename rejects blank name without closing editor`() = runTest {
        val custom = tagNode(
            id = "custom",
            name = "原名称",
            subject = "数学",
            isSystem = false,
        )
        val viewModel = TagsViewModel(FakeTagRepository(listOf(custom)))
        advanceUntilIdle()

        viewModel.onAction(TagsAction.OpenRename(custom))
        viewModel.onAction(TagsAction.EditorNameChanged("  "))
        viewModel.onAction(TagsAction.SubmitEditor)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.editor?.errorMessage).isEqualTo("请输入标签名称")
    }

    @Test
    fun `standard tag cannot open rename editor even through direct action`() = runTest {
        val standard = tagNode(
            id = "standard",
            name = "函数",
            subject = "数学",
            isSystem = true,
        )
        val viewModel = TagsViewModel(FakeTagRepository(listOf(standard)))
        advanceUntilIdle()

        viewModel.onAction(TagsAction.OpenRename(standard))

        assertThat(viewModel.uiState.value.editor).isNull()
    }
}
