package com.bandu.tiji.feature.tutor

import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeAiTutorGateway
import com.bandu.tiji.core.testing.fake.FakeTutorRepository
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
class TutorStopGenerationTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `stop cancels stream persists partial content and permits follow up`() = runTest {
        val session = sessionFixture()
        val repository = FakeTutorRepository(listOf(session))
        val gateway = FakeAiTutorGateway().apply {
            enqueueTutor(
                TutorStreamScript.Cancel(
                    listOf(AiStreamEvent.Delta("已收到的部分")),
                ),
            )
            enqueueTutor(
                TutorStreamScript.Events(
                    listOf(AiStreamEvent.Delta("后续回答"), AiStreamEvent.Completed),
                ),
            )
        }
        val viewModel = TutorSessionViewModel(repository, session.id, gateway)
        advanceUntilIdle()

        viewModel.onAction(TutorSessionAction.UpdateInput("第一次"))
        viewModel.onAction(TutorSessionAction.Send)
        runCurrent()
        advanceTimeBy(TutorSessionViewModel.STREAMING_UI_REFRESH_MILLIS)
        runCurrent()
        viewModel.onAction(TutorSessionAction.StopGeneration)
        advanceUntilIdle()

        assertThat(gateway.cancelledTutorRequests).hasSize(1)
        assertThat(repository.assistantMessages)
            .contains(session.id to "已收到的部分")
        assertThat(viewModel.uiState.value.isStreaming).isFalse()

        viewModel.onAction(TutorSessionAction.UpdateInput("继续追问"))
        viewModel.onAction(TutorSessionAction.Send)
        advanceUntilIdle()

        assertThat(gateway.tutorRequests.map { it.userMessage })
            .containsExactly("第一次", "继续追问")
            .inOrder()
        assertThat(repository.assistantMessages)
            .contains(session.id to "后续回答")
    }
}
