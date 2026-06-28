package com.bandu.tiji.feature.library

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.paging.PagingSource
import androidx.compose.ui.platform.testTag
import com.bandu.tiji.core.designsystem.theme.BanduTijiTheme
import com.bandu.tiji.core.model.enums.MasteryLevel
import com.bandu.tiji.core.model.enums.PaperLevel
import com.bandu.tiji.core.model.erroritem.ErrorItem
import com.bandu.tiji.core.model.erroritem.ErrorItemQuery
import com.bandu.tiji.core.model.erroritem.ErrorItemSummary
import com.bandu.tiji.core.model.id.CollectionId
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.model.id.TagId
import com.bandu.tiji.core.model.tag.TagSummary
import com.bandu.tiji.core.testing.fake.FakeErrorItemRepository
import com.bandu.tiji.core.testing.fixture.errorItemFixture
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class ErrorItemLibraryPerformanceScenarioTest {
    @Test
    fun `five thousand items keep paging and filtering inside feature performance gate`() = runTest {
        val repository = FakeErrorItemRepository(benchmarkItems())
        val filterQuery = ErrorItemQuery(
            collectionId = CollectionId("collection-05"),
            keyword = "quadratic",
            tagIds = setOf(TagId("tag-05")),
            gradeSemester = "高一下",
        )

        repeat(WARM_UP_RUNS) {
            repository.loadFirstPage(ErrorItemQuery())
            repository.loadNextPage(ErrorItemQuery())
            repository.loadFirstPage(filterQuery)
        }

        val firstPageTimings = measureRepeated { repository.loadFirstPage(ErrorItemQuery()) }
        val nextPageTimings = measureRepeated { repository.loadNextPage(ErrorItemQuery()) }
        val filteredTimings = measureRepeated { repository.loadFirstPage(filterQuery) }

        val firstPage = repository.loadFirstPage(ErrorItemQuery())
        val nextPage = repository.loadNextPage(ErrorItemQuery())
        val filteredPage = repository.loadFirstPage(filterQuery)

        assertThat(firstPage.data).hasSize(ErrorItemListViewModel.PAGE_SIZE)
        assertThat(nextPage.data).hasSize(ErrorItemListViewModel.PAGE_SIZE)
        assertThat(filteredPage.data).isNotEmpty()
        assertThat(filteredPage.data).hasSize(ErrorItemListViewModel.PAGE_SIZE)
        assertThat(filteredPage.data.all { item ->
            item.collectionId == CollectionId("collection-05") &&
                item.questionPreview.contains("quadratic") &&
                item.tags.any { tag -> tag.id == TagId("tag-05") }
        }).isTrue()

        assertThat(firstPageTimings.p95Millis()).isLessThan(FIRST_PAGE_GATE_MILLIS)
        assertThat(nextPageTimings.p95Millis()).isLessThan(NEXT_PAGE_GATE_MILLIS)
        assertThat(filteredTimings.p95Millis()).isLessThan(FILTER_GATE_MILLIS)
    }

    private suspend fun FakeErrorItemRepository.loadFirstPage(
        query: ErrorItemQuery,
    ): PagingSource.LoadResult.Page<Int, ErrorItemSummary> =
        page(query).load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = ErrorItemListViewModel.PAGE_SIZE,
                placeholdersEnabled = false,
            ),
        ).asPage()

    private suspend fun FakeErrorItemRepository.loadNextPage(
        query: ErrorItemQuery,
    ): PagingSource.LoadResult.Page<Int, ErrorItemSummary> {
        val source = page(query)
        val firstPage = source.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = ErrorItemListViewModel.PAGE_SIZE,
                placeholdersEnabled = false,
            ),
        ).asPage()
        return source.load(
            PagingSource.LoadParams.Append(
                key = requireNotNull(firstPage.nextKey),
                loadSize = ErrorItemListViewModel.PAGE_SIZE,
                placeholdersEnabled = false,
            ),
        ).asPage()
    }

    private suspend fun measureRepeated(
        block: suspend () -> PagingSource.LoadResult.Page<Int, ErrorItemSummary>,
    ): List<Long> =
        List(MEASURED_RUNS) {
            val startNanos = System.nanoTime()
            block()
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos)
        }

    private fun PagingSource.LoadResult<Int, ErrorItemSummary>.asPage():
        PagingSource.LoadResult.Page<Int, ErrorItemSummary> =
        this as PagingSource.LoadResult.Page<Int, ErrorItemSummary>

    private fun List<Long>.p95Millis(): Long {
        val sorted = sorted()
        val index = ceil(sorted.size * 0.95).toInt() - 1
        return sorted[index.coerceIn(sorted.indices)]
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
class ErrorItemLibraryLargeDatasetScrollTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `five thousand item list scrolls to final row`() {
        val summaries = benchmarkItems().map { it.toSummary() }

        composeRule.setContent {
            BanduTijiTheme {
                LazyColumn(modifier = Modifier.testTag("error-item-list")) {
                    items(
                        items = summaries,
                        key = { item -> item.id.value },
                    ) { item ->
                        ErrorItemCard(
                            item = item,
                            onClick = {},
                            onLongClick = {},
                            thumbnailModel = { null },
                        )
                    }
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("error-item-list").performScrollToIndex(4_999)
        composeRule.onNodeWithText("linear benchmark question 4999")
            .assertIsDisplayed()
    }
}

private const val BENCHMARK_ITEM_COUNT = 5_000
private const val WARM_UP_RUNS = 3
private const val MEASURED_RUNS = 20
private const val FIRST_PAGE_GATE_MILLIS = 500L
private const val NEXT_PAGE_GATE_MILLIS = 300L
private const val FILTER_GATE_MILLIS = 500L

private fun benchmarkItems(): List<ErrorItem> =
    List(BENCHMARK_ITEM_COUNT) { index ->
        val collectionIndex = index % 50
        val tagIndex = index % 100
        val tagId = TagId("tag-${tagIndex.toString().padStart(2, '0')}")
        val questionPrefix = if (index % 5 == 0) "quadratic" else "linear"
        errorItemFixture {
            id = ErrorItemId("benchmark-${index.toString().padStart(5, '0')}")
            collectionId = CollectionId("collection-${collectionIndex.toString().padStart(2, '0')}")
            questionText = "$questionPrefix benchmark question $index"
            answerText = "answer $index"
            analysis = "analysis $index"
            subject = if (collectionIndex % 2 == 0) "数学" else "物理"
            tags = listOf(
                TagSummary(
                    id = tagId,
                    name = "知识点 $tagIndex",
                    subject = subject,
                    isSystem = false,
                ),
            )
            gradeSemester = GRADES[index % GRADES.size]
            paperLevel = PaperLevel.entries[index % PaperLevel.entries.size]
            masteryLevel = MasteryLevel.entries[index % MasteryLevel.entries.size]
            notes = "notes $index"
            createdAtEpochMillis = index.toLong()
            updatedAtEpochMillis = index.toLong()
        }
    }

private fun ErrorItem.toSummary(): ErrorItemSummary =
    ErrorItemSummary(
        id = id,
        collectionId = collectionId,
        collectionName = collectionId.value,
        thumbnailPath = null,
        questionPreview = questionText,
        tags = tags,
        masteryLevel = masteryLevel,
        createdAtEpochMillis = createdAtEpochMillis,
    )

private val GRADES = listOf("高一上", "高一下", "高二上", "高二下")
