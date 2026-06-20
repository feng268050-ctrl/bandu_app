package com.bandu.tiji.feature.library

import com.bandu.tiji.core.model.erroritem.ErrorItemQuery
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.model.navigation.NavigationIntent

data class ErrorItemListUiState(
    val query: ErrorItemQuery = ErrorItemQuery(),
    val selectedIds: Set<ErrorItemId> = emptySet(),
    val errorMessage: String? = null,
)

sealed interface ErrorItemListAction {
    data class UpdateKeyword(
        val keyword: String,
    ) : ErrorItemListAction

    data class OpenErrorItem(
        val id: ErrorItemId,
    ) : ErrorItemListAction
}

sealed interface ErrorItemListEffect {
    data class Navigate(
        val intent: NavigationIntent,
    ) : ErrorItemListEffect
}
