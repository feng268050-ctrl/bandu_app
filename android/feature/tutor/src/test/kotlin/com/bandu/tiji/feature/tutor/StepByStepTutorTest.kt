package com.bandu.tiji.feature.tutor

import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeAiTutorGateway
import com.bandu.tiji.core.testing.fake.FakeTutorRepository
import com.bandu.tiji.core.testing.fake.FakeExerciseRepository
import com.bandu.tiji.core.testing.fake.TutorStreamScript
import com.bandu.tiji.core.testing.fixture.sessionFixture
import com.bandu.tiji.domain.ai.AiStreamEvent
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StepByStepTutorTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `step by step action sends an explicit user message`() = runTest {
        val session = sessionFixture()
        val repository = FakeTutorRepository(listOf(session))
        val gateway = FakeAiTutorGateway().apply {
            enqueueTutor(
                TutorStreamScript.Events(
                    listOf(AiStreamEvent.Delta("步骤"), AiStreamEvent.Completed),
                ),
            )
        }
        val viewModel = TutorSessionViewModel(
            repository,
            session.id,
            gateway,
            FakeExerciseRepository(),
        )
        advanceUntilIdle()

        viewModel.onAction(TutorSessionAction.RequestStepByStep)
        advanceUntilIdle()

        assertThat(repository.userMessages).containsExactly(
            session.id to TutorSessionViewModel.STEP_BY_STEP_MESSAGE,
        )
        assertThat(gateway.tutorRequests.single().userMessage)
            .isEqualTo(TutorSessionViewModel.STEP_BY_STEP_MESSAGE)
    }
}
