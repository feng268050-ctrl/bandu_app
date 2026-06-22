package com.bandu.tiji.feature.profile

import com.bandu.tiji.domain.ai.AiConfiguration
import com.bandu.tiji.domain.ai.AiConfigurationDraft
import com.bandu.tiji.domain.ai.PromptType
import com.bandu.tiji.domain.ai.ValidationResult
import com.bandu.tiji.domain.repository.AiConfigurationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal object EmptyAiConfigurationRepository : AiConfigurationRepository {
    override fun observeActiveConfiguration(): Flow<AiConfiguration?> = flowOf(null)

    override suspend fun saveAndActivate(draft: AiConfigurationDraft): ValidationResult =
        ValidationResult.Failure(listOf("AI repository is not configured"))

    override suspend fun clearApiKey() = Unit

    override suspend fun savePrompt(type: PromptType, template: String) = Unit

    override suspend fun resetPrompt(type: PromptType) = Unit
}
