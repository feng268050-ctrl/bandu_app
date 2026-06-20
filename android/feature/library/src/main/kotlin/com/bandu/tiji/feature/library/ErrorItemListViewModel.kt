package com.bandu.tiji.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.bandu.tiji.core.common.time.Clock
import com.bandu.tiji.core.common.time.SystemClock
import com.bandu.tiji.core.model.erroritem.ErrorItemQuery
import com.bandu.tiji.core.model.erroritem.ErrorItemSummary
import com.bandu.tiji.core.model.tag.TagNode
import com.bandu.tiji.core.model.navigation.NavigationIntent
import com.bandu.tiji.domain.repository.CollectionRepository
import com.bandu.tiji.domain.repository.ErrorItemRepository
import com.bandu.tiji.domain.repository.TagRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class ErrorItemListViewModel(
    private val repository: ErrorItemRepository,
    private val collectionRepository: CollectionRepository? = null,
    private val tagRepository: TagRepository? = null,
    private val clock: Clock = SystemClock(),
    initialQuery: ErrorItemQuery = ErrorItemQuery(),
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ErrorItemListUiState(query = initialQuery))
    val uiState: StateFlow<ErrorItemListUiState> = mutableUiState.asStateFlow()

    private val query = MutableStateFlow(initialQuery)
    internal val debouncedQuery: StateFlow<ErrorItemQuery> = query
        .debounce(SEARCH_DEBOUNCE_MILLIS)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = initialQuery,
        )
    val pagingData: Flow<PagingData<ErrorItemSummary>> = debouncedQuery
        .flatMapLatest { currentQuery ->
            Pager(PagingConfig(pageSize = PAGE_SIZE, prefetchDistance = PREFETCH_DISTANCE)) {
                repository.page(currentQuery)
            }.flow
        }
        .cachedIn(viewModelScope)

    private val mutableEffects = Channel<ErrorItemListEffect>(Channel.BUFFERED)
    val effects = mutableEffects.receiveAsFlow()
    private var collectionOptionsJob: Job? = null
    private var tagOptionsJob: Job? = null

    init {
        observeFilterOptions()
    }

    fun onAction(action: ErrorItemListAction) {
        when (action) {
            is ErrorItemListAction.UpdateKeyword -> {
                val updatedQuery = query.value.copy(keyword = action.keyword)
                query.value = updatedQuery
                mutableUiState.value = mutableUiState.value.copy(query = updatedQuery)
            }
            ErrorItemListAction.OpenFilters -> {
                mutableUiState.value = mutableUiState.value.copy(
                    filterDraft = mutableUiState.value.activeFilters,
                )
            }
            ErrorItemListAction.DismissFilters -> {
                mutableUiState.value = mutableUiState.value.copy(filterDraft = null)
            }
            is ErrorItemListAction.ToggleMasteryFilter -> updateFilterDraft { draft ->
                draft.copy(
                    masteryLevels = draft.masteryLevels.toggle(action.level),
                )
            }
            is ErrorItemListAction.SelectTimeRange -> updateFilterDraft {
                it.copy(timeRange = action.range)
            }
            is ErrorItemListAction.ToggleTagFilter -> updateFilterDraft { draft ->
                draft.copy(tagIds = draft.tagIds.toggle(action.id))
            }
            is ErrorItemListAction.UpdateGradeSemesterFilter -> updateFilterDraft {
                it.copy(gradeSemester = action.value)
            }
            is ErrorItemListAction.TogglePaperLevelFilter -> updateFilterDraft { draft ->
                draft.copy(paperLevels = draft.paperLevels.toggle(action.level))
            }
            is ErrorItemListAction.SelectCollectionFilter -> updateFilterDraft {
                it.copy(collectionId = action.id)
            }
            ErrorItemListAction.ApplyFilters -> applyFilterDraft()
            ErrorItemListAction.ClearFilters -> {
                mutableUiState.value = mutableUiState.value.copy(
                    filterDraft = ErrorItemFilterCriteria(),
                )
            }
            is ErrorItemListAction.OpenErrorItem -> {
                mutableEffects.trySend(
                    ErrorItemListEffect.Navigate(
                        NavigationIntent.OpenErrorItem(action.id.value),
                    ),
                )
            }
        }
    }

    private fun updateFilterDraft(
        transform: (ErrorItemFilterCriteria) -> ErrorItemFilterCriteria,
    ) {
        val draft = mutableUiState.value.filterDraft ?: return
        mutableUiState.value = mutableUiState.value.copy(filterDraft = transform(draft))
    }

    private fun applyFilterDraft() {
        val filters = mutableUiState.value.filterDraft ?: return
        val current = query.value
        val updatedQuery = current.copy(
            collectionId = filters.collectionId,
            masteryLevels = filters.masteryLevels,
            createdAfterEpochMillis = filters.timeRange.createdAfter(clock.nowEpochMillis()),
            tagIds = filters.tagIds,
            gradeSemester = filters.gradeSemester.trim().ifEmpty { null },
            paperLevels = filters.paperLevels,
        )
        query.value = updatedQuery
        mutableUiState.value = mutableUiState.value.copy(
            query = updatedQuery,
            activeFilters = filters.copy(gradeSemester = filters.gradeSemester.trim()),
            filterDraft = null,
        )
    }

    private fun observeFilterOptions() {
        collectionOptionsJob = collectionRepository?.let { source ->
            viewModelScope.launch {
                source.observeCollections()
                    .catch { emit(emptyList()) }
                    .collect { collections ->
                        mutableUiState.value = mutableUiState.value.copy(
                            availableCollections = collections,
                        )
                    }
            }
        }
        tagOptionsJob = tagRepository?.let { source ->
            viewModelScope.launch {
                source.observeTree(subject = null)
                    .catch { emit(emptyList()) }
                    .collect { tree ->
                        mutableUiState.value = mutableUiState.value.copy(
                            availableTags = tree.flattenTags(),
                        )
                    }
            }
        }
    }

    private fun ErrorItemTimeRange.createdAfter(nowEpochMillis: Long): Long? =
        when (this) {
            ErrorItemTimeRange.ALL -> null
            ErrorItemTimeRange.LAST_7_DAYS -> nowEpochMillis - 7L * MILLIS_PER_DAY
            ErrorItemTimeRange.LAST_30_DAYS -> nowEpochMillis - 30L * MILLIS_PER_DAY
        }

    private fun List<TagNode>.flattenTags(): List<com.bandu.tiji.core.model.tag.TagSummary> =
        flatMap { node -> listOf(node.tag) + node.children.flattenTags() }

    private fun <T> Set<T>.toggle(value: T): Set<T> =
        if (value in this) this - value else this + value

    companion object {
        const val PAGE_SIZE = 30
        const val PREFETCH_DISTANCE = 10
        const val SEARCH_DEBOUNCE_MILLIS = 300L
        private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1_000L
    }
}
