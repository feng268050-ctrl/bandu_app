package com.bandu.tiji.feature.tutor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun TutorSessionsRoute(
    viewModel: TutorSessionsViewModel,
    onOpenSession: (com.bandu.tiji.core.model.id.TutorSessionId) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is TutorSessionsEffect.OpenSession -> onOpenSession(effect.sessionId)
            }
        }
    }
    TutorSessionsScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
    )
}

@Composable
fun TutorSessionRoute(
    uiState: TutorSessionUiState,
    onAction: (TutorSessionAction) -> Unit,
) {
    TutorSessionScreen(
        uiState = uiState,
        onAction = onAction,
    )
}
