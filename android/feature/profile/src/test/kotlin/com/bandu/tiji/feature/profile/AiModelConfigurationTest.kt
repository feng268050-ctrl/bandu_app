package com.bandu.tiji.feature.profile

import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeAiConfigurationRepository
import com.bandu.tiji.core.testing.fake.FakeProfileRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiModelConfigurationTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `analysis and tutor models are saved independently`() = runTest {
        val repository = FakeAiConfigurationRepository()
        val viewModel = ProfileViewModel(
            profileRepository = FakeProfileRepository(),
            aiConfigurationRepository = repository,
        )
        advanceUntilIdle()

        viewModel.onAction(ProfileAction.UpdateAiDisplayName("自建服务"))
        viewModel.onAction(ProfileAction.UpdateAiBaseUrl("https://example.test/v1"))
        viewModel.onAction(ProfileAction.UpdateAiApiKey("new-secret"))
        viewModel.onAction(ProfileAction.UpdateAnalysisModel("vision-model"))
        viewModel.onAction(ProfileAction.UpdateTutorModel("reasoning-model"))
        viewModel.onAction(ProfileAction.SaveAiConfiguration)
        advanceUntilIdle()

        with(repository.savedDrafts.single()) {
            assertThat(analysisModel).isEqualTo("vision-model")
            assertThat(tutorModel).isEqualTo("reasoning-model")
            assertThat(apiKey).isEqualTo("new-secret")
        }
    }
}
