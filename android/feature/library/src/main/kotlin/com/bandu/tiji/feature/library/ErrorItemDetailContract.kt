package com.bandu.tiji.feature.library

import com.bandu.tiji.core.model.erroritem.ErrorItem
import com.bandu.tiji.core.model.navigation.NavigationIntent

data class ErrorItemDetailUiState(
    val item: ErrorItem? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

sealed interface ErrorItemDetailAction {
    data object NavigateBack : ErrorItemDetailAction
}

sealed interface ErrorItemDetailEffect {
    data object NavigateBack : ErrorItemDetailEffect

    data class Navigate(
        val intent: NavigationIntent,
    ) : ErrorItemDetailEffect
}
