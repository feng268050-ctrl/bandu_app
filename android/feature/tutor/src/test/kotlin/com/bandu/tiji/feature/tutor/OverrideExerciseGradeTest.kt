package com.bandu.tiji.feature.tutor

import com.bandu.tiji.core.model.enums.ExerciseDifficulty
import com.bandu.tiji.core.model.enums.GradeResult
import com.bandu.tiji.core.model.id.ExerciseId
import com.bandu.tiji.core.model.tutor.Exercise
import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.CallScript
import com.bandu.tiji.core.testing.fake.FakeAiTutorGateway
import com.bandu.tiji.core.testing.fake.FakeExerciseRepository
import com.bandu.tiji.core.testing.fake.FakeTutorRepository
import com.bandu.tiji.core.testing.fixture.sessionFixture
import com.bandu.tiji.domain.ai.ExerciseGrade
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OverrideExerciseGradeTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `manual override becomes final persisted result and is marked modified`() = runTest {
        val sessionId = sessionFixture().id
        val exercise = Exercise(
            id = ExerciseId("exercise-1"),
            sessionId = sessionId,
            sourceErrorItemId = null,
            subject = "数学",
            difficulty = ExerciseDifficulty.EASY,
            questionText = "1 + 1",
            expectedAnswer = "2",
            analysis = "加法",
            userAnswer = null,
            aiResult = null,
            finalResult = null,
            gradingFeedback = null,
            gradedAtEpochMillis = null,
            overriddenAtEpochMillis = null,
            createdAtEpochMillis = 0L,
        )
        val session = sessionFixture { exercises = listOf(exercise) }
        val exerciseRepository = FakeExerciseRepository(listOf(exercise))
        val gateway = FakeAiTutorGateway().apply {
            enqueueGrade(
                CallScript.Return(
                    ExerciseGrade(GradeResult.NEEDS_REVIEW, "请人工确认", 0.5),
                ),
            )
        }
        val viewModel = TutorSessionViewModel(
            FakeTutorRepository(listOf(session)),
            session.id,
            gateway,
            exerciseRepository,
        )
        advanceUntilIdle()

        viewModel.onAction(TutorSessionAction.UpdateExerciseAnswer(exercise.id, "2"))
        viewModel.onAction(TutorSessionAction.GradeExercise(exercise.id))
        advanceUntilIdle()
        viewModel.onAction(
            TutorSessionAction.OverrideGrade(exercise.id, GradeResult.CORRECT),
        )
        advanceUntilIdle()

        val state = checkNotNull(viewModel.uiState.value.exerciseGrades[exercise.id])
        assertThat(state.aiResult).isEqualTo(GradeResult.NEEDS_REVIEW)
        assertThat(state.finalResult).isEqualTo(GradeResult.CORRECT)
        assertThat(state.isOverridden).isTrue()
        assertThat(exerciseRepository.observe(exercise.id).first()?.effectiveResult)
            .isEqualTo(GradeResult.CORRECT)
    }
}
