package com.bandu.tiji.feature.profile

import com.bandu.tiji.core.model.enums.AiProviderType
import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeAiConfigurationRepository
import com.bandu.tiji.core.testing.fake.FakeProfileRepository
import com.bandu.tiji.domain.ai.AiConfiguration
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiApiKeyStateTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `saved API key is represented only as masked presence`() = runTest {
        val aiRepository = FakeAiConfigurationRepository(
            AiConfiguration(
                id = "ai-1",
                displayName = "Private service",
                providerType = AiProviderType.OPENAI_COMPATIBLE,
                baseUrl = "https://example.test/v1",
                analysisModel = "vision",
                tutorModel = "tutor",
                hasApiKey = true,
            ),
        )
        val viewModel = ProfileViewModel(
            profileRepository = FakeProfileRepository(),
            aiConfigurationRepository = aiRepository,
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.aiDraft.hasSavedApiKey).isTrue()
        assertThat(viewModel.uiState.value.aiDraft.apiKeyInput).isEmpty()
        assertThat(viewModel.uiState.value.toString()).doesNotContain("secret")
    }
}
