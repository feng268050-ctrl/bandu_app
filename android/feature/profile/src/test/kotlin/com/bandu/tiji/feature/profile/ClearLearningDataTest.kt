package com.bandu.tiji.feature.profile

import com.bandu.tiji.core.model.enums.AiProviderType
import com.bandu.tiji.core.model.profile.StudentProfile
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
class ClearLearningDataTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `typed confirmation clears learning data while profile and AI remain`() = runTest {
        val profile = StudentProfile("小伴", "高中", 2024)
        val profileRepository = FakeProfileRepository(profile)
        val aiRepository = FakeAiConfigurationRepository(
            AiConfiguration(
                id = "ai",
                displayName = "AI",
                providerType = AiProviderType.GEMINI,
                baseUrl = "https://example.test",
                analysisModel = "vision",
                tutorModel = "tutor",
                hasApiKey = true,
            ),
        )
        val viewModel = ProfileViewModel(profileRepository, aiConfigurationRepository = aiRepository)
        advanceUntilIdle()

        viewModel.onAction(ProfileAction.RequestClearLearningData)
        viewModel.onAction(
            ProfileAction.UpdateDataConfirmationText(
                ProfileViewModel.CLEAR_LEARNING_CONFIRMATION_TEXT,
            ),
        )
        viewModel.onAction(ProfileAction.ConfirmClearLearningData)
        advanceUntilIdle()

        assertThat(profileRepository.clearLearningDataCalls).isEqualTo(1)
        assertThat(viewModel.uiState.value.studentDraft.nickname).isEqualTo("小伴")
        assertThat(viewModel.uiState.value.aiDraft.hasSavedApiKey).isTrue()
        assertThat(viewModel.uiState.value.dataManagement.statusMessage)
            .isEqualTo("学习数据已清除")
    }

    @Test
    fun `mismatched text cannot clear data`() = runTest {
        val repository = FakeProfileRepository()
        val viewModel = ProfileViewModel(repository)
        advanceUntilIdle()

        viewModel.onAction(ProfileAction.RequestClearLearningData)
        viewModel.onAction(ProfileAction.UpdateDataConfirmationText("清除"))
        viewModel.onAction(ProfileAction.ConfirmClearLearningData)
        advanceUntilIdle()

        assertThat(repository.clearLearningDataCalls).isEqualTo(0)
        assertThat(viewModel.uiState.value.dataManagement.errorMessage)
            .isEqualTo("确认文本不匹配")
    }
}
