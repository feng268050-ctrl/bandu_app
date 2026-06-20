package com.bandu.tiji.feature.library

import com.bandu.tiji.core.model.erroritem.ErrorItemQuery
import com.bandu.tiji.core.model.collection.CollectionSummary
import com.bandu.tiji.core.model.enums.MasteryLevel
import com.bandu.tiji.core.model.enums.PaperLevel
import com.bandu.tiji.core.model.id.CollectionId
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.model.id.TagId
import com.bandu.tiji.core.model.navigation.NavigationIntent
import com.bandu.tiji.core.model.tag.TagSummary

data class ErrorItemListUiState(
    val query: ErrorItemQuery = ErrorItemQuery(),
    val activeFilters: ErrorItemFilterCriteria = ErrorItemFilterCriteria(),
    val filterDraft: ErrorItemFilterCriteria? = null,
    val availableCollections: List<CollectionSummary> = emptyList(),
    val availableTags: List<TagSummary> = emptyList(),
    val selectedIds: Set<ErrorItemId> = emptySet(),
    val bulkDelete: ErrorItemBulkDeleteState? = null,
    val errorMessage: String? = null,
)

data class ErrorItemBulkDeleteState(
    val ids: Set<ErrorItemId>,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null,
)

data class ErrorItemFilterCriteria(
    val masteryLevels: Set<MasteryLevel> = emptySet(),
    val timeRange: ErrorItemTimeRange = ErrorItemTimeRange.ALL,
    val tagIds: Set<TagId> = emptySet(),
    val gradeSemester: String = "",
    val paperLevels: Set<PaperLevel> = emptySet(),
    val collectionId: CollectionId? = null,
) {
    val isEmpty: Boolean
        get() = masteryLevels.isEmpty() &&
            timeRange == ErrorItemTimeRange.ALL &&
            tagIds.isEmpty() &&
            gradeSemester.isBlank() &&
            paperLevels.isEmpty() &&
            collectionId == null
}

enum class ErrorItemTimeRange {
    ALL,
    LAST_7_DAYS,
    LAST_30_DAYS,
}

sealed interface ErrorItemListAction {
    data class UpdateKeyword(
        val keyword: String,
    ) : ErrorItemListAction

    data object OpenFilters : ErrorItemListAction

    data object DismissFilters : ErrorItemListAction

    data class ToggleMasteryFilter(val level: MasteryLevel) : ErrorItemListAction

    data class SelectTimeRange(val range: ErrorItemTimeRange) : ErrorItemListAction

    data class ToggleTagFilter(val id: TagId) : ErrorItemListAction

    data class UpdateGradeSemesterFilter(val value: String) : ErrorItemListAction

    data class TogglePaperLevelFilter(val level: PaperLevel) : ErrorItemListAction

    data class SelectCollectionFilter(val id: CollectionId?) : ErrorItemListAction

    data object ApplyFilters : ErrorItemListAction

    data object ClearFilters : ErrorItemListAction

    data class ToggleSelection(val id: ErrorItemId) : ErrorItemListAction

    data class SelectAllLoaded(val ids: Set<ErrorItemId>) : ErrorItemListAction

    data object ClearSelection : ErrorItemListAction

    data object RequestBulkDelete : ErrorItemListAction

    data object ConfirmBulkDelete : ErrorItemListAction

    data object DismissBulkDelete : ErrorItemListAction

    data class OpenErrorItem(
        val id: ErrorItemId,
    ) : ErrorItemListAction
}

sealed interface ErrorItemListEffect {
    data class Navigate(
        val intent: NavigationIntent,
    ) : ErrorItemListEffect
}
