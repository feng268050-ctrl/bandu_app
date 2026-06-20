package com.bandu.tiji.feature.tags

import com.bandu.tiji.core.model.id.TagId
import com.bandu.tiji.core.model.tag.TagNode
import com.bandu.tiji.core.model.tag.TagSummary
import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeTagRepository
import com.bandu.tiji.domain.repository.TagRepository
import com.bandu.tiji.domain.tag.CreateTagInput
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TagsCreateViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `create trims name and uses selected subject and parent`() = runTest {
        val repository = FakeTagRepository()
        val viewModel = TagsViewModel(repository)
        advanceUntilIdle()
        val parentId = TagId("parent")

        viewModel.onAction(TagsAction.OpenCreate(parentId))
        viewModel.onAction(TagsAction.EditorNameChanged("  易错公式  "))
        viewModel.onAction(TagsAction.SubmitEditor)
        advanceUntilIdle()

        assertThat(repository.createdTags).containsExactly(
            CreateTagInput(
                name = "易错公式",
                subject = "数学",
                parentId = parentId,
            ),
        )
        assertThat(viewModel.uiState.value.editor).isNull()
    }

    @Test
    fun `blank name stays in editor with validation message`() = runTest {
        val viewModel = TagsViewModel(FakeTagRepository())
        advanceUntilIdle()

        viewModel.onAction(TagsAction.OpenCreate(null))
        viewModel.onAction(TagsAction.EditorNameChanged("   "))
        viewModel.onAction(TagsAction.SubmitEditor)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.editor?.errorMessage).isEqualTo("请输入标签名称")
    }

    @Test
    fun `duplicate repository failure shows same parent conflict`() = runTest {
        val repository = FakeTagRepository().apply {
            failures.enqueue(
                IllegalStateException(
                    "A tag with this subject, parent, and name already exists",
                ),
            )
        }
        val viewModel = TagsViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(TagsAction.OpenCreate(null))
        viewModel.onAction(TagsAction.EditorNameChanged("函数"))
        viewModel.onAction(TagsAction.SubmitEditor)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.editor?.errorMessage)
            .isEqualTo("同一位置已存在同名标签")
    }

    @Test
    fun `unexpected create failure keeps editor open with retryable error`() = runTest {
        val repository = FakeTagRepository().apply {
            failures.enqueue(IOException("disk unavailable"))
        }
        val viewModel = TagsViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(TagsAction.OpenCreate(null))
        viewModel.onAction(TagsAction.EditorNameChanged("函数"))
        viewModel.onAction(TagsAction.SubmitEditor)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.editor?.errorMessage).isEqualTo("无法创建标签")
        assertThat(viewModel.uiState.value.editor?.isSubmitting).isFalse()
    }

    @Test
    fun `dismissing editor cancels in flight creation`() = runTest {
        val repository = BlockingCreateTagRepository()
        val viewModel = TagsViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(TagsAction.OpenCreate(null))
        viewModel.onAction(TagsAction.EditorNameChanged("待取消"))
        viewModel.onAction(TagsAction.SubmitEditor)
        runCurrent()
        assertThat(viewModel.uiState.value.editor?.isSubmitting).isTrue()

        viewModel.onAction(TagsAction.DismissEditor)
        advanceUntilIdle()

        assertThat(repository.createCancelled.await()).isTrue()
        assertThat(viewModel.uiState.value.editor).isNull()
    }
}

private class BlockingCreateTagRepository : TagRepository {
    val createCancelled = CompletableDeferred<Boolean>()

    override fun observeTree(subject: String?): Flow<List<TagNode>> = flowOf(emptyList())

    override suspend fun findTag(id: TagId): TagSummary? = null

    override suspend fun createCustom(input: CreateTagInput): TagId =
        try {
            CompletableDeferred<TagId>().await()
        } finally {
            createCancelled.complete(true)
        }

    override suspend fun renameCustom(id: TagId, name: String) = Unit

    override suspend fun deleteCustom(id: TagId) = Unit
}
