package com.bandu.tiji.feature.library

import com.bandu.tiji.core.model.collection.CollectionSummary
import com.bandu.tiji.core.model.id.CollectionId
import com.bandu.tiji.core.model.navigation.NavigationIntent

data class CollectionListUiState(
    val collections: List<CollectionSummary> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

sealed interface CollectionListAction {
    data object Retry : CollectionListAction

    data class OpenCollection(
        val id: CollectionId,
    ) : CollectionListAction
}

sealed interface CollectionListEffect {
    data class Navigate(
        val intent: NavigationIntent,
    ) : CollectionListEffect
}
