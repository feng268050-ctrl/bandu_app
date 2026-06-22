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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TutorStreamingTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `stream deltas refresh UI in 250 millisecond batches`() = runTest {
        val session = sessionFixture()
        val repository = FakeTutorRepository(listOf(session))
        val gateway = FakeAiTutorGateway().apply {
            enqueueTutor(
                TutorStreamScript.Cancel(
                    eventsBeforeCancellation = listOf(
                        AiStreamEvent.Delta("第一步"),
                        AiStreamEvent.Delta("\n第二步"),
                    ),
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
        viewModel.onAction(TutorSessionAction.UpdateInput("请讲解"))
        viewModel.onAction(TutorSessionAction.Send)
        runCurrent()

        assertThat(viewModel.uiState.value.streamingText).isEmpty()
        advanceTimeBy(249L)
        runCurrent()
        assertThat(viewModel.uiState.value.streamingText).isEmpty()
        advanceTimeBy(1L)
        runCurrent()
        assertThat(viewModel.uiState.value.streamingText).isEqualTo("第一步\n第二步")
        assertThat(gateway.tutorRequests.single().userMessage).isEqualTo("请讲解")
    }

    @Test
    fun `completed stream persists assistant message`() = runTest {
        val session = sessionFixture()
        val repository = FakeTutorRepository(listOf(session))
        val gateway = FakeAiTutorGateway().apply {
            enqueueTutor(
                TutorStreamScript.Events(
                    listOf(
                        AiStreamEvent.Delta("完整"),
                        AiStreamEvent.Delta("回答"),
                        AiStreamEvent.Completed,
                    ),
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
        viewModel.onAction(TutorSessionAction.UpdateInput("问题"))
        viewModel.onAction(TutorSessionAction.Send)
        advanceUntilIdle()

        assertThat(repository.userMessages).containsExactly(session.id to "问题")
        assertThat(repository.assistantMessages).containsExactly(session.id to "完整回答")
        assertThat(viewModel.uiState.value.isStreaming).isFalse()
    }
}
