package com.bandu.tiji.feature.tutor

import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeAiTutorGateway
import com.bandu.tiji.core.testing.fake.FakeExerciseRepository
import com.bandu.tiji.core.testing.fake.FakeTutorRepository
import com.bandu.tiji.core.testing.fake.TutorStreamScript
import com.bandu.tiji.core.testing.fixture.sessionFixture
import com.bandu.tiji.domain.ai.AiGatewayException
import com.bandu.tiji.domain.ai.AiStreamEvent
import com.bandu.tiji.domain.pending.PendingAiOperation
import com.bandu.tiji.domain.pending.PendingOperationCoordinator
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TutorPendingOperationTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `configuration required message resumes exactly once`() = runTest {
        val session = sessionFixture()
        val coordinator = PendingOperationCoordinator()
        val gateway = FakeAiTutorGateway().apply {
            enqueueTutor(
                TutorStreamScript.Fail(
                    throwable = AiGatewayException.ConfigurationRequired,
                ),
            )
            enqueueTutor(
                TutorStreamScript.Events(
                    listOf(AiStreamEvent.Delta("恢复回答"), AiStreamEvent.Completed),
                ),
            )
        }
        val viewModel = TutorSessionViewModel(
            FakeTutorRepository(listOf(session)),
            session.id,
            gateway,
            FakeExerciseRepository(),
            coordinator,
        )
        advanceUntilIdle()

        val effect = async { viewModel.effects.first() }
        viewModel.onAction(TutorSessionAction.UpdateInput("原问题"))
        viewModel.onAction(TutorSessionAction.Send)
        advanceUntilIdle()
        assertThat(effect.await()).isEqualTo(TutorSessionEffect.OpenAiConfiguration)

        val pending = coordinator.consumeForResume()
        assertThat(pending).isEqualTo(
            PendingAiOperation.SendTutorMessage(session.id, "原问题"),
        )
        viewModel.onAction(TutorSessionAction.ResumePending(checkNotNull(pending)))
        advanceUntilIdle()
        viewModel.onAction(TutorSessionAction.ResumePending(pending))
        advanceUntilIdle()

        assertThat(gateway.tutorRequests.map { it.userMessage })
            .containsExactly("原问题", "原问题")
        assertThat(coordinator.consumeForResume()).isNull()
    }
}
