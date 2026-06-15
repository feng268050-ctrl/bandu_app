package com.bandu.tiji.core.testing.fake

import com.bandu.tiji.domain.ai.AiConfiguration
import com.bandu.tiji.domain.ai.AiConfigurationDraft
import com.bandu.tiji.domain.ai.PromptType
import com.bandu.tiji.domain.ai.ValidationResult
import com.bandu.tiji.domain.repository.AiConfigurationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAiConfigurationRepository(
    initialConfiguration: AiConfiguration? = null,
) : AiConfigurationRepository {
    private val activeConfiguration = MutableStateFlow(initialConfiguration)
    private var nextId = 1

    val failures = FailureInjector()
    val savedDrafts = mutableListOf<AiConfigurationDraft>()
    val savedPrompts = mutableMapOf<PromptType, String>()
    val resetPrompts = mutableListOf<PromptType>()
    var validationResult: ValidationResult = ValidationResult.Success
    var clearApiKeyCalls: Int = 0
        private set

    override fun observeActiveConfiguration(): Flow<AiConfiguration?> =
        activeConfiguration.asStateFlow()

    override suspend fun saveAndActivate(draft: AiConfigurationDraft): ValidationResult {
        failures.throwIfQueued()
        savedDrafts += draft
        val result = validationResult
        if (result is ValidationResult.Success) {
            activeConfiguration.value =
                AiConfiguration(
                    id = activeConfiguration.value?.id ?: "configuration-${nextId++}",
                    displayName = draft.displayName,
                    providerType = draft.providerType,
                    baseUrl = draft.baseUrl,
                    analysisModel = draft.analysisModel,
                    tutorModel = draft.tutorModel,
                    hasApiKey =
                        !draft.apiKey.isNullOrBlank() || activeConfiguration.value?.hasApiKey == true,
                )
        }
        return result
    }

    override suspend fun clearApiKey() {
        failures.throwIfQueued()
        clearApiKeyCalls += 1
        activeConfiguration.value =
            activeConfiguration.value?.copy(hasApiKey = false)
    }

    override suspend fun savePrompt(type: PromptType, template: String) {
        failures.throwIfQueued()
        savedPrompts[type] = template
    }

    override suspend fun resetPrompt(type: PromptType) {
        failures.throwIfQueued()
        savedPrompts.remove(type)
        resetPrompts += type
    }

    fun emit(configuration: AiConfiguration?) {
        activeConfiguration.value = configuration
    }
}
