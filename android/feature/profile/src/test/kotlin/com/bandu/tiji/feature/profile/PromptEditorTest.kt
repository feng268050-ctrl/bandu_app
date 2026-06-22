package com.bandu.tiji.feature.profile

import com.bandu.tiji.core.testing.coroutines.MainDispatcherRule
import com.bandu.tiji.core.testing.fake.FakeAiConfigurationRepository
import com.bandu.tiji.core.testing.fake.FakeProfileRepository
import com.bandu.tiji.domain.ai.PromptType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PromptEditorTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `missing placeholders are rejected and draft remains editable`() = runTest {
        val repository = FakeAiConfigurationRepository()
        val viewModel = ProfileViewModel(
            profileRepository = FakeProfileRepository(),
            aiConfigurationRepository = repository,
        )
        advanceUntilIdle()

        viewModel.onAction(ProfileAction.SelectPromptType(PromptType.TUTOR))
        viewModel.onAction(ProfileAction.UpdatePromptTemplate("只包含 {{user_message}}"))
        viewModel.onAction(ProfileAction.SavePrompt)
        advanceUntilIdle()

        assertThat(repository.savedPrompts).isEmpty()
        assertThat(viewModel.uiState.value.promptEditor.template)
            .isEqualTo("只包含 {{user_message}}")
        assertThat(viewModel.uiState.value.promptEditor.errorMessage)
            .isEqualTo("提示词缺少必要占位符或包含未知占位符")
    }

    @Test
    fun `valid templates save and reset to the matching default`() = runTest {
        val repository = FakeAiConfigurationRepository()
        val viewModel = ProfileViewModel(
            profileRepository = FakeProfileRepository(),
            aiConfigurationRepository = repository,
        )
        advanceUntilIdle()

        viewModel.onAction(ProfileAction.SelectPromptType(PromptType.GRADE_EXERCISE))
        val custom = "请批改\n${PromptDefaults.getValue(PromptType.GRADE_EXERCISE)}"
        viewModel.onAction(ProfileAction.UpdatePromptTemplate(custom))
        viewModel.onAction(ProfileAction.SavePrompt)
        advanceUntilIdle()
        assertThat(repository.savedPrompts[PromptType.GRADE_EXERCISE]).isEqualTo(custom)

        viewModel.onAction(ProfileAction.ResetPrompt)
        advanceUntilIdle()
        assertThat(repository.resetPrompts).containsExactly(PromptType.GRADE_EXERCISE)
        assertThat(viewModel.uiState.value.promptEditor.template)
            .isEqualTo(PromptDefaults.getValue(PromptType.GRADE_EXERCISE))
    }
}
