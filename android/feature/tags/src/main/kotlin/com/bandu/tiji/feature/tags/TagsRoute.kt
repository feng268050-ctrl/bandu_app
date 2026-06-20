package com.bandu.tiji.feature.tags

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun TagsRoute(
    viewModel: TagsViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    TagsScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
    )
}
