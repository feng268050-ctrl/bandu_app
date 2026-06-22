package com.bandu.tiji.feature.tutor

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TutorContractTest {
    @Test
    fun `contracts start with deterministic loading states`() {
        assertThat(TutorSessionsUiState()).isEqualTo(
            TutorSessionsUiState(
                sessions = emptyList(),
                isLoading = true,
                isCreating = false,
                errorMessage = null,
                pendingDelete = null,
            ),
        )
        assertThat(TutorSessionUiState()).isEqualTo(
            TutorSessionUiState(
                session = null,
                input = "",
                streamingText = "",
                isLoading = true,
                isStreaming = false,
                errorMessage = null,
                selectedDifficulty = null,
                exerciseAnswers = emptyMap(),
                gradingExerciseIds = emptySet(),
                pendingDelete = false,
            ),
        )
    }
}
