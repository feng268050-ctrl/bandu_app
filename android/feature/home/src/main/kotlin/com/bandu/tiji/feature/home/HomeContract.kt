package com.bandu.tiji.feature.home

import com.bandu.tiji.core.model.navigation.NavigationIntent

data class HomeUiState(
    val nickname: String? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

sealed interface HomeAction {
    data object RetryProfile : HomeAction

    data class OpenDestination(
        val intent: NavigationIntent,
    ) : HomeAction
}

sealed interface HomeEffect {
    data class Navigate(
        val intent: NavigationIntent,
    ) : HomeEffect
}
