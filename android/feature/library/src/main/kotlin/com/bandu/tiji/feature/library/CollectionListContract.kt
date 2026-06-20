package com.bandu.tiji.feature.library

import com.bandu.tiji.core.model.collection.CollectionSummary
import com.bandu.tiji.core.model.id.CollectionId
import com.bandu.tiji.core.model.navigation.NavigationIntent

data class CollectionListUiState(
    val collections: List<CollectionSummary> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val editor: CollectionEditorUiState? = null,
)

data class CollectionEditorUiState(
    val mode: CollectionEditorMode,
    val name: String,
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
)

sealed interface CollectionEditorMode {
    data object Create : CollectionEditorMode

    data class Rename(
        val id: CollectionId,
    ) : CollectionEditorMode
}

sealed interface CollectionListAction {
    data object Retry : CollectionListAction

    data object RequestCreate : CollectionListAction

    data class RequestRename(
        val id: CollectionId,
    ) : CollectionListAction

    data class UpdateEditorName(
        val name: String,
    ) : CollectionListAction

    data object SubmitEditor : CollectionListAction

    data object DismissEditor : CollectionListAction

    data class OpenCollection(
        val id: CollectionId,
    ) : CollectionListAction
}

sealed interface CollectionListEffect {
    data class Navigate(
        val intent: NavigationIntent,
    ) : CollectionListEffect
}
