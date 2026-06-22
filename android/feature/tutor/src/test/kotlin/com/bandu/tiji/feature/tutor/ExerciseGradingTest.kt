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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseGradingTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `AI grading persists correct incorrect and needs review states`() = runTest {
        val exercise = exercise()
        val session = sessionFixture { exercises = listOf(exercise) }
        val tutorRepository = FakeTutorRepository(listOf(session))
        val exerciseRepository = FakeExerciseRepository(listOf(exercise))
        val gateway = FakeAiTutorGateway().apply {
            GradeResult.entries.forEach { result ->
                enqueueGrade(
                    CallScript.Return(
                        ExerciseGrade(result, "feedback-${result.name}", 0.9),
                    ),
                )
            }
        }
        val viewModel = TutorSessionViewModel(
            tutorRepository,
            session.id,
            gateway,
            exerciseRepository,
        )
        advanceUntilIdle()

        GradeResult.entries.forEach { result ->
            viewModel.onAction(
                TutorSessionAction.UpdateExerciseAnswer(exercise.id, "answer-${result.name}"),
            )
            viewModel.onAction(TutorSessionAction.GradeExercise(exercise.id))
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.exerciseGrades[exercise.id]?.aiResult)
                .isEqualTo(result)
        }

        assertThat(gateway.gradeRequests.map { it.userAnswer })
            .containsExactly(
                "answer-CORRECT",
                "answer-INCORRECT",
                "answer-NEEDS_REVIEW",
            )
            .inOrder()
        assertThat(exerciseRepository.observe(exercise.id).first()?.aiResult)
            .isEqualTo(GradeResult.NEEDS_REVIEW)
    }

    private fun exercise() =
        Exercise(
            id = ExerciseId("exercise-1"),
            sessionId = sessionFixture().id,
            sourceErrorItemId = null,
            subject = "数学",
            difficulty = ExerciseDifficulty.MEDIUM,
            questionText = "1 + 1 = ?",
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
}
