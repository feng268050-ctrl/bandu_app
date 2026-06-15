package com.bandu.tiji.domain.usecase

import com.bandu.tiji.core.common.result.AppError
import com.bandu.tiji.core.model.enums.AiProviderType
import com.bandu.tiji.domain.ai.AiConfigurationDraft
import com.bandu.tiji.domain.ai.PromptType
import com.bandu.tiji.domain.ai.PromptValidator
import com.bandu.tiji.domain.ai.ValidationResult
import com.bandu.tiji.domain.fake.FakeAiConfigurationRepository
import com.bandu.tiji.domain.usecase.aiconfig.ResetPromptUseCase
import com.bandu.tiji.domain.usecase.aiconfig.SaveAndActivateAiConfigurationUseCase
import com.bandu.tiji.domain.usecase.aiconfig.SavePromptUseCase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AiConfigUseCaseTest {
    private val repository = FakeAiConfigurationRepository()

    private fun validDraft() = AiConfigurationDraft(
        providerType = AiProviderType.GEMINI,
        displayName = "Gemini",
        baseUrl = "https://generativelanguage.googleapis.com",
        apiKey = "secret",
        analysisModel = "gemini-2.0-flash",
        tutorModel = "gemini-2.0-flash",
    )

    private fun validTutorTemplate() =
        "{{question_context}} {{conversation_context}} {{user_message}} {{grade_instruction}}"

    @Test
    fun saveAndActivateAiConfiguration_succeedsOnValidation() = runTest {
        val result = SaveAndActivateAiConfigurationUseCase(repository).invoke(validDraft())

        assertThat(result.isSuccess).isTrue()
        assertThat(repository.savedDraft).isEqualTo(validDraft())
    }

    @Test
    fun saveAndActivateAiConfiguration_mapsRepositoryFailure() = runTest {
        repository.validationResult = ValidationResult.Failure(listOf("auth failed"))

        val result = SaveAndActivateAiConfigurationUseCase(repository).invoke(validDraft())

        assertThat(result.isSuccess).isFalse()
        val error = (result as com.bandu.tiji.core.common.result.AppResult.Failure).error
        assertThat((error as AppError.Validation).code).contains("auth failed")
    }

    @Test
    fun savePrompt_rejectsMissingPlaceholders() = runTest {
        val result = SavePromptUseCase(repository).invoke(
            PromptType.TUTOR,
            "no placeholders here",
        )

        assertThat(result.isSuccess).isFalse()
        assertThat(repository.savedPrompts).isEmpty()
    }

    @Test
    fun savePrompt_acceptsValidTemplate() = runTest {
        val result = SavePromptUseCase(repository).invoke(PromptType.TUTOR, validTutorTemplate())

        assertThat(result.isSuccess).isTrue()
        assertThat(repository.savedPrompts[PromptType.TUTOR]).isEqualTo(validTutorTemplate())
    }

    @Test
    fun promptValidator_rejectsUnknownPlaceholder() {
        val result = PromptValidator().validate(
            PromptType.TUTOR,
            "${validTutorTemplate()} {{unknown_var}}",
        )

        assertThat(result.isSuccess).isFalse()
    }

    @Test
    fun resetPrompt_delegatesToRepository() = runTest {
        val result = ResetPromptUseCase(repository).invoke(PromptType.ANALYZE_IMAGE)

        assertThat(result.isSuccess).isTrue()
        assertThat(repository.resetPrompts).containsExactly(PromptType.ANALYZE_IMAGE)
    }
}
