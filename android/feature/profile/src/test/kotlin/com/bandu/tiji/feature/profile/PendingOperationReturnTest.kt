package com.bandu.tiji.feature.profile

import com.bandu.tiji.core.model.id.TutorSessionId
import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeAiConfigurationRepository
import com.bandu.tiji.core.testing.fake.FakeProfileRepository
import com.bandu.tiji.domain.pending.PendingAiOperation
import com.bandu.tiji.domain.pending.PendingOperationCoordinator
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PendingOperationReturnTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `validated configuration returns capture operation once`() = runTest {
        val operation = PendingAiOperation.AnalyzeCapture("draft-1")
        val coordinator = PendingOperationCoordinator().apply { save(operation) }
        val viewModel = configuredViewModel(coordinator)

        val effect = async {
            viewModel.effects.filterIsInstance<ProfileEffect.ConfigurationActivated>().first()
        }
        viewModel.onAction(ProfileAction.SaveAiConfiguration)
        advanceUntilIdle()

        assertThat(effect.await().pendingOperation).isEqualTo(operation)
        assertThat(coordinator.consumeForResume()).isNull()
    }

    @Test
    fun `validated configuration returns tutor operation once`() = runTest {
        val operation = PendingAiOperation.SendTutorMessage(
            TutorSessionId("session-1"),
            "继续讲解",
        )
        val coordinator = PendingOperationCoordinator().apply { save(operation) }
        val viewModel = configuredViewModel(coordinator)

        val effect = async {
            viewModel.effects.filterIsInstance<ProfileEffect.ConfigurationActivated>().first()
        }
        viewModel.onAction(ProfileAction.SaveAiConfiguration)
        advanceUntilIdle()

        assertThat(effect.await().pendingOperation).isEqualTo(operation)
    }

    private suspend fun kotlinx.coroutines.test.TestScope.configuredViewModel(
        coordinator: PendingOperationCoordinator,
    ): ProfileViewModel {
        val viewModel = ProfileViewModel(
            profileRepository = FakeProfileRepository(),
            aiConfigurationRepository = FakeAiConfigurationRepository(),
            pendingOperations = coordinator,
        )
        runCurrent()
        viewModel.onAction(ProfileAction.UpdateAiDisplayName("AI"))
        viewModel.onAction(ProfileAction.UpdateAiBaseUrl("https://example.test"))
        viewModel.onAction(ProfileAction.UpdateAiApiKey("secret"))
        viewModel.onAction(ProfileAction.UpdateAnalysisModel("vision"))
        viewModel.onAction(ProfileAction.UpdateTutorModel("tutor"))
        return viewModel
    }
}
