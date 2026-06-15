package com.bandu.tiji.core.testing.fake

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.bandu.tiji.core.model.erroritem.ErrorItem
import com.bandu.tiji.core.model.erroritem.ErrorItemDraft
import com.bandu.tiji.core.model.erroritem.ErrorItemPatch
import com.bandu.tiji.core.model.erroritem.ErrorItemQuery
import com.bandu.tiji.core.model.erroritem.ErrorItemSummary
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.model.tag.TagSummary
import com.bandu.tiji.core.testing.fixture.DEFAULT_FIXTURE_EPOCH_MILLIS
import com.bandu.tiji.domain.repository.ErrorItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class FakeErrorItemRepository(
    initialItems: List<ErrorItem> = emptyList(),
) : ErrorItemRepository {
    private val items = MutableStateFlow(initialItems.associateBy(ErrorItem::id))
    private var nextId = initialItems.size + 1

    val failures = FailureInjector()
    val pageQueries = mutableListOf<ErrorItemQuery>()
    val createdDrafts = mutableListOf<ErrorItemDraft>()
    val updatedItems = mutableListOf<Pair<ErrorItemId, ErrorItemPatch>>()
    val deletedIdSets = mutableListOf<Set<ErrorItemId>>()

    override fun page(query: ErrorItemQuery): PagingSource<Int, ErrorItemSummary> {
        failures.throwIfQueued()
        pageQueries += query
        val snapshot =
            items.value.values
                .filter { query.matches(it) }
                .map { it.toSummary() }
        return ListPagingSource(snapshot)
    }

    override fun observe(id: ErrorItemId): Flow<ErrorItem?> =
        items
            .map { it[id] }
            .distinctUntilChanged()

    override suspend fun create(draft: ErrorItemDraft): ErrorItemId {
        failures.throwIfQueued()
        createdDrafts += draft
        val id = ErrorItemId("error-item-${nextId++}")
        val item =
            ErrorItem(
                id = id,
                collectionId = draft.collectionId,
                image = draft.image,
                questionText = draft.questionText,
                answerText = draft.answerText,
                analysis = draft.analysis,
                wrongAnswerText = draft.wrongAnswerText,
                mistakeStatus = draft.mistakeStatus,
                mistakeAnalysis = draft.mistakeAnalysis,
                subject = draft.subject,
                tags = draft.tagIds.map { TagSummary(it, it.value, draft.subject, false) },
                gradeSemester = draft.gradeSemester,
                paperLevel = draft.paperLevel,
                notes = draft.notes,
                masteryLevel = draft.masteryLevel,
                createdAtEpochMillis = DEFAULT_FIXTURE_EPOCH_MILLIS,
                updatedAtEpochMillis = DEFAULT_FIXTURE_EPOCH_MILLIS,
            )
        items.value += id to item
        return id
    }

    override suspend fun update(id: ErrorItemId, patch: ErrorItemPatch) {
        failures.throwIfQueued()
        updatedItems += id to patch
        items.value[id]?.let { current ->
            items.value += id to current.apply(patch)
        }
    }

    override suspend fun delete(ids: Set<ErrorItemId>) {
        failures.throwIfQueued()
        deletedIdSets += ids
        items.value = items.value - ids
    }

    fun emit(item: ErrorItem) {
        items.value += item.id to item
    }

    fun emit(items: List<ErrorItem>) {
        this.items.value = items.associateBy(ErrorItem::id)
    }

    private fun ErrorItemQuery.matches(item: ErrorItem): Boolean {
        val searchableText = "${item.questionText} ${item.answerText} ${item.analysis} ${item.notes}"
        val createdAfter = createdAfterEpochMillis
        return (collectionId == null || item.collectionId == collectionId) &&
            (keyword.isBlank() || searchableText.contains(keyword, ignoreCase = true)) &&
            (masteryLevels.isEmpty() || item.masteryLevel in masteryLevels) &&
            (createdAfter == null || item.createdAtEpochMillis >= createdAfter) &&
            (tagIds.isEmpty() || item.tags.map { it.id }.containsAll(tagIds)) &&
            (gradeSemester == null || item.gradeSemester == gradeSemester) &&
            (paperLevels.isEmpty() || item.paperLevel in paperLevels)
    }

    private fun ErrorItem.toSummary(): ErrorItemSummary =
        ErrorItemSummary(
            id = id,
            collectionId = collectionId,
            collectionName = collectionId.value,
            thumbnailPath = image?.thumbnailRelativePath,
            questionPreview = questionText,
            tags = tags,
            masteryLevel = masteryLevel,
            createdAtEpochMillis = createdAtEpochMillis,
        )

    private fun ErrorItem.apply(patch: ErrorItemPatch): ErrorItem =
        copy(
            collectionId = patch.collectionId ?: collectionId,
            image = patch.image ?: image,
            questionText = patch.questionText ?: questionText,
            answerText = patch.answerText ?: answerText,
            analysis = patch.analysis ?: analysis,
            wrongAnswerText = patch.wrongAnswerText ?: wrongAnswerText,
            mistakeStatus = patch.mistakeStatus ?: mistakeStatus,
            mistakeAnalysis = patch.mistakeAnalysis ?: mistakeAnalysis,
            subject = patch.subject ?: subject,
            tags =
                patch.tagIds?.map { TagSummary(it, it.value, patch.subject ?: subject, false) }
                    ?: tags,
            gradeSemester = patch.gradeSemester ?: gradeSemester,
            paperLevel = patch.paperLevel ?: paperLevel,
            notes = patch.notes ?: notes,
            masteryLevel = patch.masteryLevel ?: masteryLevel,
            updatedAtEpochMillis = DEFAULT_FIXTURE_EPOCH_MILLIS,
        )

    private class ListPagingSource(
        private val data: List<ErrorItemSummary>,
    ) : PagingSource<Int, ErrorItemSummary>() {
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ErrorItemSummary> {
            val start = params.key ?: 0
            val end = minOf(start + params.loadSize, data.size)
            val page = if (start >= data.size) emptyList() else data.subList(start, end)
            return LoadResult.Page(
                data = page,
                prevKey = if (start == 0) null else maxOf(0, start - params.loadSize),
                nextKey = if (end >= data.size) null else end,
            )
        }

        override fun getRefreshKey(state: PagingState<Int, ErrorItemSummary>): Int? =
            state.anchorPosition
    }
}
