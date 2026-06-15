package com.bandu.tiji.domain.usecase.aiconfig

import com.bandu.tiji.domain.util.runSuspendCatching

import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.domain.ai.PromptType
import com.bandu.tiji.domain.ai.PromptValidator
import com.bandu.tiji.domain.repository.AiConfigurationRepository

class SavePromptUseCase(
    private val repository: AiConfigurationRepository,
    private val promptValidator: PromptValidator = PromptValidator(),
) {
    suspend operator fun invoke(type: PromptType, template: String): AppResult<Unit> =
        when (val validated = promptValidator.validate(type, template)) {
            is AppResult.Success -> runSuspendCatching { repository.savePrompt(type, validated.value) }
            is AppResult.Failure -> validated
        }
}
