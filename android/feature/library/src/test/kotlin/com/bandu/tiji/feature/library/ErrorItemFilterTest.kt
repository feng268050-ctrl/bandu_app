package com.bandu.tiji.feature.library

import com.bandu.tiji.core.model.collection.CollectionSummary
import com.bandu.tiji.core.model.enums.MasteryLevel
import com.bandu.tiji.core.model.enums.PaperLevel
import com.bandu.tiji.core.model.id.CollectionId
import com.bandu.tiji.core.model.id.TagId
import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeCollectionRepository
import com.bandu.tiji.core.testing.fake.FakeErrorItemRepository
import com.bandu.tiji.core.testing.fake.FakeTagRepository
import com.bandu.tiji.core.testing.time.TestClock
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ErrorItemFilterTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `all filters combine into one error item query`() = runTest {
        val collectionId = CollectionId("collection-1")
        val tagId = TagId("tag-1")
        val now = 1_800_000_000_000L
        val viewModel = ErrorItemListViewModel(
            repository = FakeErrorItemRepository(),
            collectionRepository = FakeCollectionRepository(
                listOf(
                    CollectionSummary(collectionId, "期中", 10, now),
                ),
            ),
            tagRepository = FakeTagRepository(
                listOf(tagNode(tagId)),
            ),
            clock = TestClock(now),
        )
        advanceUntilIdle()

        viewModel.onAction(ErrorItemListAction.UpdateKeyword("函数"))
        viewModel.onAction(ErrorItemListAction.OpenFilters)
        viewModel.onAction(ErrorItemListAction.ToggleMasteryFilter(MasteryLevel.REVIEWING))
        viewModel.onAction(ErrorItemListAction.SelectTimeRange(ErrorItemTimeRange.LAST_7_DAYS))
        viewModel.onAction(ErrorItemListAction.ToggleTagFilter(tagId))
        viewModel.onAction(ErrorItemListAction.UpdateGradeSemesterFilter(" 八年级上 "))
        viewModel.onAction(ErrorItemListAction.TogglePaperLevelFilter(PaperLevel.A))
        viewModel.onAction(ErrorItemListAction.SelectCollectionFilter(collectionId))
        viewModel.onAction(ErrorItemListAction.ApplyFilters)

        val query = viewModel.uiState.value.query
        assertThat(query.keyword).isEqualTo("函数")
        assertThat(query.collectionId).isEqualTo(collectionId)
        assertThat(query.masteryLevels).containsExactly(MasteryLevel.REVIEWING)
        assertThat(query.createdAfterEpochMillis)
            .isEqualTo(now - 7L * 24L * 60L * 60L * 1_000L)
        assertThat(query.tagIds).containsExactly(tagId)
        assertThat(query.gradeSemester).isEqualTo("八年级上")
        assertThat(query.paperLevels).containsExactly(PaperLevel.A)
        assertThat(viewModel.uiState.value.availableCollections.single().name).isEqualTo("期中")
        assertThat(viewModel.uiState.value.availableTags.single().name).isEqualTo("函数")
    }

    @Test
    fun `clear filters preserves keyword and removes every filter`() = runTest {
        val viewModel = ErrorItemListViewModel(FakeErrorItemRepository())

        viewModel.onAction(ErrorItemListAction.UpdateKeyword("解析"))
        viewModel.onAction(ErrorItemListAction.OpenFilters)
        viewModel.onAction(ErrorItemListAction.ToggleMasteryFilter(MasteryLevel.MASTERED))
        viewModel.onAction(ErrorItemListAction.ClearFilters)
        viewModel.onAction(ErrorItemListAction.ApplyFilters)

        assertThat(viewModel.uiState.value.query.keyword).isEqualTo("解析")
        assertThat(viewModel.uiState.value.activeFilters.isEmpty).isTrue()
        assertThat(viewModel.uiState.value.query.masteryLevels).isEmpty()
    }

    private fun tagNode(id: TagId) =
        com.bandu.tiji.core.model.tag.TagNode(
            tag = com.bandu.tiji.core.model.tag.TagSummary(
                id = id,
                name = "函数",
                subject = "数学",
                isSystem = true,
            ),
            code = "MATH-FUNCTION",
            sortOrder = 0,
            linkedErrorItemCount = 1,
            children = emptyList(),
        )
}
