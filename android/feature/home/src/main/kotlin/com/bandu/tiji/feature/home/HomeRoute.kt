package com.bandu.tiji.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.bandu.tiji.core.model.navigation.NavigationIntent

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onNavigate: (NavigationIntent) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is HomeEffect.Navigate -> onNavigate(effect.intent)
            }
        }
    }
    HomeScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
    )
}
