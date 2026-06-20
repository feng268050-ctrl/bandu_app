package com.bandu.tiji.feature.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.bandu.tiji.core.model.navigation.NavigationIntent

@Composable
fun CollectionListRoute(
    viewModel: CollectionListViewModel,
    onNavigate: (NavigationIntent) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CollectionListEffect.Navigate -> onNavigate(effect.intent)
            }
        }
    }
    CollectionListScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
    )
}

@Composable
fun ErrorItemListRoute(
    uiState: ErrorItemListUiState,
    onAction: (ErrorItemListAction) -> Unit,
    content: @Composable (ErrorItemListUiState, (ErrorItemListAction) -> Unit) -> Unit,
) {
    content(uiState, onAction)
}

@Composable
fun ErrorItemDetailRoute(
    uiState: ErrorItemDetailUiState,
    onAction: (ErrorItemDetailAction) -> Unit,
    content: @Composable (ErrorItemDetailUiState, (ErrorItemDetailAction) -> Unit) -> Unit,
) {
    content(uiState, onAction)
}
