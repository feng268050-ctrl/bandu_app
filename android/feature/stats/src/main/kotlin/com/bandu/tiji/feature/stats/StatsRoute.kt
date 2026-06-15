package com.bandu.tiji.feature.stats

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun StatsRoute(
    viewModel: StatsViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    StatsScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
    )
}
