package com.bandu.tiji.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect

@Composable
fun ProfileRoute(
    viewModel: ProfileViewModel,
    onAiDataConsentGranted: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ProfileEffect.AiDataConsentGranted -> onAiDataConsentGranted()
            }
        }
    }
    ProfileScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
    )
}
