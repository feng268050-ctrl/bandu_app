package com.bandu.tiji.domain.usecase.aiconfig

import com.bandu.tiji.domain.util.runSuspendCatching

import com.bandu.tiji.core.common.result.AppResult
import com.bandu.tiji.domain.ai.PromptType
import com.bandu.tiji.domain.repository.AiConfigurationRepository

class ResetPromptUseCase(
    private val repository: AiConfigurationRepository,
) {
    suspend operator fun invoke(type: PromptType): AppResult<Unit> =
        runSuspendCatching { repository.resetPrompt(type) }
}
