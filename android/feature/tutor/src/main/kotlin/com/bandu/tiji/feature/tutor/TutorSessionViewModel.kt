package com.bandu.tiji.feature.tutor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bandu.tiji.core.model.id.TutorSessionId
import com.bandu.tiji.domain.repository.TutorRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class TutorSessionViewModel(
    private val tutorRepository: TutorRepository,
    private val sessionId: TutorSessionId,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(TutorSessionUiState())
    val uiState: StateFlow<TutorSessionUiState> = mutableUiState.asStateFlow()

    private val mutableEffects = Channel<TutorSessionEffect>(Channel.BUFFERED)
    val effects = mutableEffects.receiveAsFlow()

    private var observationJob: Job? = null

    init {
        observeSession()
    }

    fun onAction(action: TutorSessionAction) {
        when (action) {
            TutorSessionAction.Retry -> observeSession()
            TutorSessionAction.NavigateBack ->
                mutableEffects.trySend(TutorSessionEffect.NavigateBack)
            is TutorSessionAction.UpdateInput ->
                mutableUiState.value = mutableUiState.value.copy(input = action.text)
            TutorSessionAction.Send,
            TutorSessionAction.StopGeneration,
            TutorSessionAction.RequestStepByStep,
            is TutorSessionAction.SelectDifficulty,
            TutorSessionAction.GenerateExercise,
            is TutorSessionAction.UpdateExerciseAnswer,
            is TutorSessionAction.GradeExercise,
            is TutorSessionAction.OverrideGrade,
            TutorSessionAction.RequestDelete,
            TutorSessionAction.DismissDelete,
            TutorSessionAction.ConfirmDelete,
            -> Unit
        }
    }

    private fun observeSession() {
        observationJob?.cancel()
        mutableUiState.value = mutableUiState.value.copy(
            isLoading = true,
            errorMessage = null,
        )
        observationJob = viewModelScope.launch {
            tutorRepository.observeSession(sessionId)
                .catch {
                    mutableUiState.value = mutableUiState.value.copy(
                        session = null,
                        isLoading = false,
                        errorMessage = "无法加载辅导会话",
                    )
                }
                .collect { session ->
                    mutableUiState.value = mutableUiState.value.copy(
                        session = session,
                        isLoading = false,
                        errorMessage = if (session == null) "会话不存在" else null,
                    )
                }
        }
    }
}
