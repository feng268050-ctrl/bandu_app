package com.bandu.tiji.feature.tutor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bandu.tiji.core.model.id.TutorSessionId
import com.bandu.tiji.domain.repository.TutorRepository
import com.bandu.tiji.domain.repository.AiTutorGateway
import com.bandu.tiji.domain.repository.ExerciseRepository
import com.bandu.tiji.domain.usecase.tutor.SendTutorMessageUseCase
import com.bandu.tiji.domain.usecase.tutor.StopTutorGenerationUseCase
import com.bandu.tiji.domain.usecase.tutor.GenerateExerciseUseCase
import com.bandu.tiji.domain.usecase.tutor.GradeExerciseUseCase
import com.bandu.tiji.domain.ai.ExerciseRequest
import com.bandu.tiji.domain.ai.GradeExerciseRequest
import com.bandu.tiji.core.model.tutor.ExerciseDraft
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
    private val exerciseRepository: ExerciseRepository,
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
    private val generateExercise = GenerateExerciseUseCase(aiTutorGateway)
    private val gradeExercise = GradeExerciseUseCase(aiTutorGateway)

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
            TutorSessionAction.RequestStepByStep -> send(STEP_BY_STEP_MESSAGE)
            is TutorSessionAction.SelectDifficulty ->
                mutableUiState.value = mutableUiState.value.copy(
                    selectedDifficulty = action.difficulty,
                    exerciseErrorMessage = null,
                )
            TutorSessionAction.GenerateExercise -> generateExercise()
            is TutorSessionAction.UpdateExerciseAnswer ->
                mutableUiState.value = mutableUiState.value.copy(
                    exerciseAnswers = mutableUiState.value.exerciseAnswers +
                        (action.exerciseId to action.text),
                    exerciseErrorMessage = null,
                )
            is TutorSessionAction.GradeExercise -> gradeExercise(action.exerciseId)
            is TutorSessionAction.OverrideGrade ->
                overrideGrade(action.exerciseId, action.result)
            TutorSessionAction.RequestDelete,
            TutorSessionAction.DismissDelete,
            TutorSessionAction.ConfirmDelete,
            -> Unit
        }
    }

    private fun send(quickMessage: String? = null) {
        val state = mutableUiState.value
        val session = state.session ?: return
        val input = (quickMessage ?: state.input).trim()
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

    private fun generateExercise() {
        val state = mutableUiState.value
        val session = state.session ?: return
        val difficulty = state.selectedDifficulty ?: return
        if (state.isGeneratingExercise) return
        mutableUiState.value = state.copy(
            isGeneratingExercise = true,
            exerciseErrorMessage = null,
        )
        viewModelScope.launch {
            val request = ExerciseRequest(
                sessionId = sessionId,
                originalQuestion = session.messages
                    .lastOrNull { it.role == com.bandu.tiji.core.model.tutor.TutorMessageRole.USER }
                    ?.content
                    .orEmpty(),
                knowledgePoints = "",
                difficulty = difficulty,
                sourceErrorItemId = session.errorItemId,
                subject = "未分类",
            )
            when (val result = generateExercise(request)) {
                is AppResult.Success -> {
                    val generated = result.value
                    exerciseRepository.create(
                        ExerciseDraft(
                            sessionId = sessionId,
                            sourceErrorItemId = session.errorItemId,
                            subject = "未分类",
                            difficulty = difficulty,
                            questionText = generated.questionText,
                            expectedAnswer = generated.answerText,
                            analysis = generated.analysis,
                        ),
                    )
                    mutableUiState.value = mutableUiState.value.copy(
                        isGeneratingExercise = false,
                    )
                }
                is AppResult.Failure -> mutableUiState.value = mutableUiState.value.copy(
                    isGeneratingExercise = false,
                    exerciseErrorMessage = "无法生成类似练习",
                )
            }
        }
    }

    private fun gradeExercise(exerciseId: com.bandu.tiji.core.model.id.ExerciseId) {
        val state = mutableUiState.value
        val exercise = state.session?.exercises?.firstOrNull { it.id == exerciseId } ?: return
        val answer = state.exerciseAnswers[exerciseId].orEmpty()
        if (exerciseId in state.gradingExerciseIds) return
        mutableUiState.value = state.copy(
            gradingExerciseIds = state.gradingExerciseIds + exerciseId,
            exerciseErrorMessage = null,
        )
        viewModelScope.launch {
            when (
                val result = gradeExercise(
                    GradeExerciseRequest(
                        exerciseId = exercise.id,
                        exerciseQuestion = exercise.questionText,
                        expectedAnswer = exercise.expectedAnswer,
                        userAnswer = answer,
                        rubricContext = exercise.analysis,
                    ),
                )
            ) {
                is AppResult.Success -> {
                    val grade = result.value
                    exerciseRepository.recordGrade(
                        id = exercise.id,
                        userAnswer = answer.trim(),
                        result = grade.result,
                        feedback = grade.feedback,
                    )
                    mutableUiState.value = mutableUiState.value.copy(
                        gradingExerciseIds =
                            mutableUiState.value.gradingExerciseIds - exerciseId,
                        exerciseGrades = mutableUiState.value.exerciseGrades +
                            (
                                exerciseId to ExerciseGradeUiState(
                                    aiResult = grade.result,
                                    feedback = grade.feedback,
                                )
                            ),
                    )
                }
                is AppResult.Failure -> mutableUiState.value = mutableUiState.value.copy(
                    gradingExerciseIds =
                        mutableUiState.value.gradingExerciseIds - exerciseId,
                    exerciseErrorMessage =
                        if (answer.isBlank()) "请输入练习答案" else "无法批改练习",
                )
            }
        }
    }

    private fun overrideGrade(
        exerciseId: com.bandu.tiji.core.model.id.ExerciseId,
        result: com.bandu.tiji.core.model.enums.GradeResult,
    ) {
        val current = mutableUiState.value.exerciseGrades[exerciseId] ?: return
        viewModelScope.launch {
            runCatching {
                exerciseRepository.overrideResult(exerciseId, result)
            }.onSuccess {
                mutableUiState.value = mutableUiState.value.copy(
                    exerciseGrades = mutableUiState.value.exerciseGrades +
                        (
                            exerciseId to current.copy(
                                finalResult = result,
                                isOverridden = true,
                            )
                        ),
                    exerciseErrorMessage = null,
                )
            }.onFailure {
                mutableUiState.value = mutableUiState.value.copy(
                    exerciseErrorMessage = "无法修改批改结果",
                )
            }
        }
    }

    companion object {
        const val STREAMING_UI_REFRESH_MILLIS = 250L
        const val STEP_BY_STEP_MESSAGE = "请分步骤讲解这道题，并说明每一步使用的知识点。"
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
