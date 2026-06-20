package com.bandu.tiji.feature.library

import androidx.compose.runtime.Composable

@Composable
fun CollectionListRoute(
    uiState: CollectionListUiState,
    onAction: (CollectionListAction) -> Unit,
    content: @Composable (CollectionListUiState, (CollectionListAction) -> Unit) -> Unit,
) {
    content(uiState, onAction)
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
