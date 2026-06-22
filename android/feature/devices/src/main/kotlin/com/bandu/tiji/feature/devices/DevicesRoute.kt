package com.bandu.tiji.feature.devices

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun DevicesRoute(
    viewModel: DevicesViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    DevicesScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
    )
}
