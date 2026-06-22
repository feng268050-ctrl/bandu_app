package com.bandu.tiji.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import com.bandu.tiji.domain.pending.PendingAiOperation

@Composable
fun ProfileRoute(
    viewModel: ProfileViewModel,
    onAiDataConsentGranted: () -> Unit = {},
    onConfigurationActivated: (PendingAiOperation?) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ProfileEffect.AiDataConsentGranted -> onAiDataConsentGranted()
                is ProfileEffect.ConfigurationActivated ->
                    onConfigurationActivated(effect.pendingOperation)
            }
        }
    }
    ProfileScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
    )
}
