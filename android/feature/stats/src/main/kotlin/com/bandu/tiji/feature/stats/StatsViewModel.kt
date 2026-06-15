package com.bandu.tiji.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bandu.tiji.domain.repository.StatsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class StatsViewModel(
    private val statsRepository: StatsRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = mutableUiState.asStateFlow()

    private var statsJob: Job? = null
    private var wrongItemsLoaded = false
    private var exercisesLoaded = false
    private var wrongItemsFailed = false
    private var exercisesFailed = false

    init {
        loadStats()
    }

    fun onAction(action: StatsAction) {
        when (action) {
            StatsAction.Retry -> loadStats()
        }
    }

    private fun loadStats() {
        statsJob?.cancel()
        wrongItemsLoaded = false
        exercisesLoaded = false
        wrongItemsFailed = false
        exercisesFailed = false
        mutableUiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
            )
        }
        statsJob = viewModelScope.launch {
            supervisorScope {
                launch {
                    statsRepository.observeWrongItemStats()
                        .catch {
                            wrongItemsLoaded = true
                            wrongItemsFailed = true
                            updateLoadStatus()
                        }
                        .collect { stats ->
                            wrongItemsLoaded = true
                            wrongItemsFailed = false
                            mutableUiState.update { state ->
                                state.copy(wrongItemStats = stats)
                            }
                            updateLoadStatus()
                        }
                }
                launch {
                    statsRepository.observeExerciseStats()
                        .catch {
                            exercisesLoaded = true
                            exercisesFailed = true
                            updateLoadStatus()
                        }
                        .collect { stats ->
                            exercisesLoaded = true
                            exercisesFailed = false
                            mutableUiState.update { state ->
                                state.copy(exerciseStats = stats)
                            }
                            updateLoadStatus()
                        }
                }
            }
        }
    }

    private fun updateLoadStatus() {
        mutableUiState.update { state ->
            state.copy(
                isLoading = !(wrongItemsLoaded && exercisesLoaded),
                errorMessage = if (wrongItemsFailed || exercisesFailed) {
                    STATS_LOAD_ERROR
                } else {
                    null
                },
            )
        }
    }

    private companion object {
        const val STATS_LOAD_ERROR = "无法加载统计数据"
    }
}
