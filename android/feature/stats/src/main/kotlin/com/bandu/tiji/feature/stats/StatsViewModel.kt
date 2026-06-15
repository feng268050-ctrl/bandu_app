package com.bandu.tiji.feature.stats

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StatsViewModel : ViewModel() {
    private val mutableUiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = mutableUiState.asStateFlow()

    fun onAction(action: StatsAction) = Unit
}
