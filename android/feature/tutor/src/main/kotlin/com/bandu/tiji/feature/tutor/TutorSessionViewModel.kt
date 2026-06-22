package com.bandu.tiji.feature.tutor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bandu.tiji.core.model.id.TutorSessionId
import com.bandu.tiji.domain.repository.TutorRepository
import com.bandu.tiji.domain.repository.AiTutorGateway
import com.bandu.tiji.domain.usecase.tutor.SendTutorMessageUseCase
import com.bandu.tiji.domain.usecase.tutor.StopTutorGenerationUseCase
import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.domain.ai.AiStreamEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancelAndJoin
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
    aiTutorGateway: AiTutorGateway,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(TutorSessionUiState())
    val uiState: StateFlow<TutorSessionUiState> = mutableUiState.asStateFlow()

    private val mutableEffects = Channel<TutorSessionEffect>(Channel.BUFFERED)
    val effects = mutableEffects.receiveAsFlow()

    private var observationJob: Job? = null
    private var streamingJob: Job? = null
    private var refreshJob: Job? = null
    private var stopJob: Job? = null
    private var pendingStreamingText = ""
    private val sendTutorMessage = SendTutorMessageUseCase(tutorRepository, aiTutorGateway)
    private val stopTutorGeneration = StopTutorGenerationUseCase(tutorRepository)

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
            TutorSessionAction.Send -> send()
            TutorSessionAction.StopGeneration -> stopGeneration()
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

    private fun send() {
        val state = mutableUiState.value
        val session = state.session ?: return
        val input = state.input.trim()
        if (input.isEmpty() || state.isStreaming) return
        pendingStreamingText = ""
        mutableUiState.value = state.copy(
            input = "",
            streamingText = "",
            isStreaming = true,
            errorMessage = null,
        )
        streamingJob = viewModelScope.launch {
            sendTutorMessage.invoke(
                sessionId = sessionId,
                text = input,
                questionContext = "",
                conversationContext = session.messages
                    .takeLast(20)
                    .joinToString("\n") { "${it.role}: ${it.content}" },
            ).collect { result ->
                when (result) {
                    is AppResult.Success -> when (val event = result.value) {
                        is AiStreamEvent.Delta -> {
                            pendingStreamingText += event.text
                            scheduleStreamingRefresh()
                        }
                        AiStreamEvent.Completed -> {
                            refreshJob?.cancel()
                            mutableUiState.value = mutableUiState.value.copy(
                                streamingText = "",
                                isStreaming = false,
                            )
                            pendingStreamingText = ""
                        }
                        is AiStreamEvent.Usage -> Unit
                    }
                    is AppResult.Failure -> {
                        refreshJob?.cancel()
                        mutableUiState.value = mutableUiState.value.copy(
                            streamingText = pendingStreamingText,
                            isStreaming = false,
                            errorMessage = "AI 辅导失败，请重试",
                        )
                    }
                }
            }
        }
    }

    private fun scheduleStreamingRefresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            delay(STREAMING_UI_REFRESH_MILLIS)
            mutableUiState.value = mutableUiState.value.copy(
                streamingText = pendingStreamingText,
            )
        }
    }

    private fun stopGeneration() {
        if (!mutableUiState.value.isStreaming) return
        val partialText = pendingStreamingText
        stopJob?.cancel()
        stopJob = viewModelScope.launch {
            streamingJob?.cancelAndJoin()
            refreshJob?.cancel()
            if (partialText.isNotBlank()) {
                stopTutorGeneration(sessionId, partialText)
            }
            pendingStreamingText = ""
            mutableUiState.value = mutableUiState.value.copy(
                streamingText = "",
                isStreaming = false,
                errorMessage = null,
            )
        }
    }

    companion object {
        const val STREAMING_UI_REFRESH_MILLIS = 250L
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
