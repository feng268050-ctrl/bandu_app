package com.bandu.tiji.feature.tutor

import com.bandu.tiji.core.model.enums.ExerciseDifficulty
import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.CallScript
import com.bandu.tiji.core.testing.fake.FakeAiTutorGateway
import com.bandu.tiji.core.testing.fake.FakeExerciseRepository
import com.bandu.tiji.core.testing.fake.FakeTutorRepository
import com.bandu.tiji.core.testing.fixture.sessionFixture
import com.bandu.tiji.domain.ai.GeneratedExercise
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GenerateExerciseTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `all four difficulties generate and persist session exercises`() = runTest {
        val session = sessionFixture()
        val tutorRepository = FakeTutorRepository(listOf(session))
        val exerciseRepository = FakeExerciseRepository()
        val gateway = FakeAiTutorGateway().apply {
            ExerciseDifficulty.entries.forEach { difficulty ->
                enqueueExercise(
                    CallScript.Return(
                        GeneratedExercise(
                            questionText = "${difficulty.name} question",
                            answerText = "answer",
                            analysis = "analysis",
                        ),
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

        ExerciseDifficulty.entries.forEach { difficulty ->
            viewModel.onAction(TutorSessionAction.SelectDifficulty(difficulty))
            viewModel.onAction(TutorSessionAction.GenerateExercise)
            advanceUntilIdle()
        }

        assertThat(gateway.exerciseRequests.map { it.difficulty })
            .containsExactlyElementsIn(ExerciseDifficulty.entries)
            .inOrder()
        assertThat(exerciseRepository.createdDrafts.map { it.difficulty })
            .containsExactlyElementsIn(ExerciseDifficulty.entries)
            .inOrder()
        assertThat(exerciseRepository.createdDrafts.map { it.sessionId }.distinct())
            .containsExactly(session.id)
    }
}
