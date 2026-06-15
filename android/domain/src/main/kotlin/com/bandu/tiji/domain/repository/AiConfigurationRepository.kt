package com.bandu.tiji.domain.repository

import com.bandu.tiji.domain.ai.AiConfiguration
import com.bandu.tiji.domain.ai.AiConfigurationDraft
import com.bandu.tiji.domain.ai.PromptType
import com.bandu.tiji.domain.ai.ValidationResult
import kotlinx.coroutines.flow.Flow

interface AiConfigurationRepository {
    fun observeActiveConfiguration(): Flow<AiConfiguration?>

    suspend fun saveAndActivate(draft: AiConfigurationDraft): ValidationResult

    suspend fun clearApiKey()

    suspend fun savePrompt(type: PromptType, template: String)

    suspend fun resetPrompt(type: PromptType)
}
