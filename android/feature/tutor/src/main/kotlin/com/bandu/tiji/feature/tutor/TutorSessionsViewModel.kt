package com.bandu.tiji.feature.tutor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.domain.repository.TutorRepository
import com.bandu.tiji.domain.usecase.tutor.GetOrCreateTutorSessionUseCase
import com.bandu.tiji.domain.usecase.tutor.DeleteTutorSessionUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class TutorSessionsViewModel(
    private val repository: TutorRepository,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(TutorSessionsUiState())
    val uiState: StateFlow<TutorSessionsUiState> = mutableUiState.asStateFlow()

    private val mutableEffects = Channel<TutorSessionsEffect>(Channel.BUFFERED)
    val effects = mutableEffects.receiveAsFlow()

    private val getOrCreateSession = GetOrCreateTutorSessionUseCase(repository)
    private val deleteSession = DeleteTutorSessionUseCase(repository)
    private var observationJob: Job? = null
    private var createJob: Job? = null
    private var deleteJob: Job? = null

    init {
        observeSessions()
    }

    fun onAction(action: TutorSessionsAction) {
        when (action) {
            TutorSessionsAction.Retry -> observeSessions()
            TutorSessionsAction.CreateSession -> createSession(null)
            is TutorSessionsAction.CreateSessionForError -> createSession(action.errorItemId)
            is TutorSessionsAction.OpenSession ->
                mutableEffects.trySend(TutorSessionsEffect.OpenSession(action.sessionId))
            is TutorSessionsAction.RequestDelete -> {
                mutableUiState.value = mutableUiState.value.copy(
                    pendingDelete = action.sessionId,
                    deleteErrorMessage = null,
                )
            }
            TutorSessionsAction.DismissDelete -> {
                if (!mutableUiState.value.isDeleting) {
                    mutableUiState.value = mutableUiState.value.copy(
                        pendingDelete = null,
                        deleteErrorMessage = null,
                    )
                }
            }
            TutorSessionsAction.ConfirmDelete -> confirmDelete()
        }
    }

    private fun observeSessions() {
        observationJob?.cancel()
        mutableUiState.value = mutableUiState.value.copy(
            isLoading = true,
            errorMessage = null,
        )
        observationJob = viewModelScope.launch {
            repository.observeSessions()
                .catch {
                    mutableUiState.value = mutableUiState.value.copy(
                        isLoading = false,
                        errorMessage = "无法加载辅导会话",
                    )
                }
                .collect { sessions ->
                    mutableUiState.value = mutableUiState.value.copy(
                        sessions = sessions,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
        }
    }

    private fun createSession(errorItemId: ErrorItemId?) {
        if (mutableUiState.value.isCreating) return
        mutableUiState.value = mutableUiState.value.copy(
            isCreating = true,
            errorMessage = null,
        )
        createJob = viewModelScope.launch {
            when (val result = getOrCreateSession(errorItemId)) {
                is AppResult.Success -> {
                    mutableUiState.value = mutableUiState.value.copy(isCreating = false)
                    mutableEffects.send(TutorSessionsEffect.OpenSession(result.value))
                }
                is AppResult.Failure -> {
                    mutableUiState.value = mutableUiState.value.copy(
                        isCreating = false,
                        errorMessage = "无法创建辅导会话",
                    )
                }
            }
        }
    }

    private fun confirmDelete() {
        val sessionId = mutableUiState.value.pendingDelete ?: return
        if (mutableUiState.value.isDeleting) return
        mutableUiState.value = mutableUiState.value.copy(
            isDeleting = true,
            deleteErrorMessage = null,
        )
        deleteJob?.cancel()
        deleteJob = viewModelScope.launch {
            when (deleteSession(sessionId)) {
                is AppResult.Success -> mutableUiState.value = mutableUiState.value.copy(
                    pendingDelete = null,
                    isDeleting = false,
                    deleteErrorMessage = null,
                )
                is AppResult.Failure -> mutableUiState.value = mutableUiState.value.copy(
                    isDeleting = false,
                    deleteErrorMessage = "无法删除辅导会话",
                )
            }
        }
    }
}
