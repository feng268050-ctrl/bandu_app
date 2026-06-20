package com.bandu.tiji.feature.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.paging.compose.collectAsLazyPagingItems
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
    viewModel: ErrorItemListViewModel,
    onNavigate: (NavigationIntent) -> Unit,
    thumbnailModel: (String) -> Any? = { it },
) {
    val uiState by viewModel.uiState.collectAsState()
    val items = viewModel.pagingData.collectAsLazyPagingItems()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ErrorItemListEffect.Navigate -> onNavigate(effect.intent)
            }
        }
    }
    ErrorItemListScreen(
        uiState = uiState,
        items = items,
        onAction = viewModel::onAction,
        thumbnailModel = thumbnailModel,
    )
}

@Composable
fun ErrorItemDetailRoute(
    uiState: ErrorItemDetailUiState,
    onAction: (ErrorItemDetailAction) -> Unit,
    content: @Composable (ErrorItemDetailUiState, (ErrorItemDetailAction) -> Unit) -> Unit,
) {
    content(uiState, onAction)
}
