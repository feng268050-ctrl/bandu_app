package com.bandu.tiji.feature.tutor

import com.bandu.tiji.core.model.enums.ExerciseDifficulty
import com.bandu.tiji.core.model.enums.GradeResult
import com.bandu.tiji.core.model.id.ExerciseId
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.model.id.TutorSessionId
import com.bandu.tiji.core.model.tutor.TutorSession
import com.bandu.tiji.core.model.tutor.TutorSessionSummary

data class TutorSessionsUiState(
    val sessions: List<TutorSessionSummary> = emptyList(),
    val isLoading: Boolean = true,
    val isCreating: Boolean = false,
    val errorMessage: String? = null,
    val pendingDelete: TutorSessionId? = null,
)

sealed interface TutorSessionsAction {
    data object Retry : TutorSessionsAction
    data object CreateSession : TutorSessionsAction
    data class CreateSessionForError(val errorItemId: ErrorItemId) : TutorSessionsAction
    data class OpenSession(val sessionId: TutorSessionId) : TutorSessionsAction
    data class RequestDelete(val sessionId: TutorSessionId) : TutorSessionsAction
    data object DismissDelete : TutorSessionsAction
    data object ConfirmDelete : TutorSessionsAction
}

sealed interface TutorSessionsEffect {
    data class OpenSession(val sessionId: TutorSessionId) : TutorSessionsEffect
}

data class TutorSessionUiState(
    val session: TutorSession? = null,
    val input: String = "",
    val streamingText: String = "",
    val isLoading: Boolean = true,
    val isStreaming: Boolean = false,
    val errorMessage: String? = null,
    val selectedDifficulty: ExerciseDifficulty? = null,
    val exerciseAnswers: Map<ExerciseId, String> = emptyMap(),
    val gradingExerciseIds: Set<ExerciseId> = emptySet(),
    val pendingDelete: Boolean = false,
)

sealed interface TutorSessionAction {
    data object Retry : TutorSessionAction
    data object NavigateBack : TutorSessionAction
    data class UpdateInput(val text: String) : TutorSessionAction
    data object Send : TutorSessionAction
    data object StopGeneration : TutorSessionAction
    data object RequestStepByStep : TutorSessionAction
    data class SelectDifficulty(val difficulty: ExerciseDifficulty) : TutorSessionAction
    data object GenerateExercise : TutorSessionAction
    data class UpdateExerciseAnswer(
        val exerciseId: ExerciseId,
        val text: String,
    ) : TutorSessionAction
    data class GradeExercise(val exerciseId: ExerciseId) : TutorSessionAction
    data class OverrideGrade(
        val exerciseId: ExerciseId,
        val result: GradeResult,
    ) : TutorSessionAction
    data object RequestDelete : TutorSessionAction
    data object DismissDelete : TutorSessionAction
    data object ConfirmDelete : TutorSessionAction
}

sealed interface TutorSessionEffect {
    data object NavigateBack : TutorSessionEffect
    data object OpenAiConfiguration : TutorSessionEffect
}
