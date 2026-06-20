package com.bandu.tiji.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.domain.repository.ErrorItemRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class ErrorItemDetailViewModel(
    private val repository: ErrorItemRepository,
    private val errorItemId: ErrorItemId,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(ErrorItemDetailUiState())
    val uiState: StateFlow<ErrorItemDetailUiState> = mutableUiState.asStateFlow()

    private val mutableEffects = Channel<ErrorItemDetailEffect>(Channel.BUFFERED)
    val effects = mutableEffects.receiveAsFlow()
    private var observationJob: Job? = null

    init {
        observeItem()
    }

    fun onAction(action: ErrorItemDetailAction) {
        when (action) {
            ErrorItemDetailAction.NavigateBack ->
                mutableEffects.trySend(ErrorItemDetailEffect.NavigateBack)
            ErrorItemDetailAction.Retry -> observeItem()
        }
    }

    private fun observeItem() {
        observationJob?.cancel()
        mutableUiState.value = ErrorItemDetailUiState()
        observationJob = viewModelScope.launch {
            repository.observe(errorItemId)
                .catch {
                    mutableUiState.value = ErrorItemDetailUiState(
                        isLoading = false,
                        errorMessage = "无法加载错题详情",
                    )
                }
                .collect { item ->
                    mutableUiState.value = ErrorItemDetailUiState(
                        item = item,
                        isLoading = false,
                        errorMessage = if (item == null) "错题不存在" else null,
                    )
                }
        }
    }
}
