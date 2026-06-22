package com.bandu.tiji.feature.profile

import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeAiConfigurationRepository
import com.bandu.tiji.core.testing.fake.FakeProfileRepository
import com.bandu.tiji.domain.ai.ValidationResult
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiConnectionValidationTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `successful validation activates configuration and clears plaintext key`() = runTest {
        val repository = FakeAiConfigurationRepository()
        val viewModel = configuredViewModel(repository)

        viewModel.onAction(ProfileAction.SaveAiConfiguration)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isAiConfigurationActive).isTrue()
        assertThat(viewModel.uiState.value.aiDraft.apiKeyInput).isEmpty()
        assertThat(viewModel.uiState.value.aiDraft.hasSavedApiKey).isTrue()
        assertThat(viewModel.uiState.value.aiValidationMessage)
            .isEqualTo("连接验证成功，配置已激活")
    }

    @Test
    fun `failed validation preserves inactive draft for correction`() = runTest {
        val repository = FakeAiConfigurationRepository().apply {
            validationResult = ValidationResult.Failure(listOf("authentication"))
        }
        val viewModel = configuredViewModel(repository)

        viewModel.onAction(ProfileAction.SaveAiConfiguration)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isAiConfigurationActive).isFalse()
        assertThat(viewModel.uiState.value.aiDraft.apiKeyInput).isEqualTo("draft-secret")
        assertThat(viewModel.uiState.value.aiDraft.analysisModel).isEqualTo("vision")
        assertThat(viewModel.uiState.value.aiDraft.tutorModel).isEqualTo("tutor")
        assertThat(viewModel.uiState.value.aiValidationMessage)
            .isEqualTo("连接验证失败，请检查地址、模型和密钥")
    }

    private suspend fun kotlinx.coroutines.test.TestScope.configuredViewModel(
        repository: FakeAiConfigurationRepository,
    ): ProfileViewModel {
        val viewModel = ProfileViewModel(
            profileRepository = FakeProfileRepository(),
            aiConfigurationRepository = repository,
        )
        advanceUntilIdle()
        viewModel.onAction(ProfileAction.UpdateAiDisplayName("服务"))
        viewModel.onAction(ProfileAction.UpdateAiBaseUrl("https://example.test/v1"))
        viewModel.onAction(ProfileAction.UpdateAiApiKey("draft-secret"))
        viewModel.onAction(ProfileAction.UpdateAnalysisModel("vision"))
        viewModel.onAction(ProfileAction.UpdateTutorModel("tutor"))
        return viewModel
    }
}
