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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TagsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial load observes mathematics tree`() = runTest {
        val mathematics = tagNode(id = "math", name = "数学", subject = "数学")
        val repository = FakeTagRepository(listOf(mathematics))

        val viewModel = TagsViewModel(repository)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.tree).containsExactly(mathematics)
        assertThat(viewModel.uiState.value.isLoading).isFalse()
    }

    @Test
    fun `selecting another subject replaces tree and clears expansion`() = runTest {
        val mathematics = tagNode(id = "math", name = "函数", subject = "数学")
        val physics = tagNode(id = "physics", name = "力学", subject = "物理")
        val repository = FakeTagRepository(listOf(mathematics, physics))
        val viewModel = TagsViewModel(repository)
        advanceUntilIdle()
        viewModel.onAction(TagsAction.ToggleExpanded(mathematics.tag.id))

        viewModel.onAction(TagsAction.SelectSubject("物理"))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.selectedSubject).isEqualTo("物理")
        assertThat(viewModel.uiState.value.tree).containsExactly(physics)
        assertThat(viewModel.uiState.value.expandedTagIds).isEmpty()
    }

    @Test
    fun `switching subjects cancels the previous observation`() = runTest {
        val repository = CancellableTagRepository()
        val viewModel = TagsViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(TagsAction.SelectSubject("物理"))
        advanceUntilIdle()

        assertThat(repository.cancelledSubjects).containsExactly("数学")
        assertThat(viewModel.uiState.value.tree.single().tag.subject).isEqualTo("物理")
    }

    @Test
    fun `load failure can be retried`() = runTest {
        val repository = RecoveringTagRepository()
        val viewModel = TagsViewModel(repository)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.loadErrorMessage).isEqualTo("无法加载标签")

        viewModel.onAction(TagsAction.Retry)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.loadErrorMessage).isNull()
        assertThat(viewModel.uiState.value.tree.single().tag.name).isEqualTo("代数")
        assertThat(repository.observationCount).isEqualTo(2)
    }

    @Test
    fun `toggle expansion adds and removes the same tag`() = runTest {
        val root = tagNode(id = "root", name = "代数", subject = "数学")
        val viewModel = TagsViewModel(FakeTagRepository(listOf(root)))
        advanceUntilIdle()

        viewModel.onAction(TagsAction.ToggleExpanded(root.tag.id))
        assertThat(viewModel.uiState.value.expandedTagIds).containsExactly(root.tag.id)

        viewModel.onAction(TagsAction.ToggleExpanded(root.tag.id))
        assertThat(viewModel.uiState.value.expandedTagIds).isEmpty()
    }
}

private class CancellableTagRepository : NoOpTagRepository() {
    val cancelledSubjects = mutableListOf<String>()

    override fun observeTree(subject: String?): Flow<List<TagNode>> = flow {
        if (subject == "数学") {
            try {
                awaitCancellation()
            } finally {
                cancelledSubjects += "数学"
            }
        }
        emit(listOf(tagNode(id = "physics", name = "力学", subject = requireNotNull(subject))))
    }
}

private class RecoveringTagRepository : NoOpTagRepository() {
    var observationCount = 0

    override fun observeTree(subject: String?): Flow<List<TagNode>> = flow {
        observationCount += 1
        if (observationCount == 1) throw IOException("unavailable")
        emit(listOf(tagNode(id = "algebra", name = "代数", subject = requireNotNull(subject))))
    }
}

private open class NoOpTagRepository : TagRepository {
    override fun observeTree(subject: String?): Flow<List<TagNode>> = flow { emit(emptyList()) }

    override suspend fun findTag(id: TagId): TagSummary? = null

    override suspend fun createCustom(input: CreateTagInput): TagId = TagId("unused")

    override suspend fun renameCustom(id: TagId, name: String) = Unit

    override suspend fun deleteCustom(id: TagId) = Unit
}

internal fun tagNode(
    id: String,
    name: String,
    subject: String,
    isSystem: Boolean = true,
    code: String? = null,
    count: Int = 0,
    children: List<TagNode> = emptyList(),
): TagNode =
    TagNode(
        tag = TagSummary(TagId(id), name, subject, isSystem),
        code = code,
        sortOrder = 0,
        linkedErrorItemCount = count,
        children = children,
    )
