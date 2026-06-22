package com.bandu.tiji.feature.tutor

import androidx.compose.runtime.Composable

@Composable
fun TutorSessionsRoute(
    uiState: TutorSessionsUiState,
    onAction: (TutorSessionsAction) -> Unit,
) {
    TutorSessionsScreen(
        uiState = uiState,
        onAction = onAction,
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
