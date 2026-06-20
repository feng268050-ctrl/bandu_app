package com.bandu.tiji.data.repository

import com.bandu.tiji.core.storage.preferences.DevicePreferences
import com.bandu.tiji.core.storage.preferences.DevicePreferencesStore
import com.bandu.tiji.core.storage.preferences.PortablePreferences
import com.bandu.tiji.core.storage.preferences.PortablePreferencesStore
import com.bandu.tiji.data.ai.AiConfigurationValidationGateway
import com.bandu.tiji.data.ai.ApiKeyCipherStore
import com.bandu.tiji.domain.ai.AiConfiguration
import com.bandu.tiji.domain.ai.AiConfigurationDraft
import com.bandu.tiji.domain.ai.PromptType
import com.bandu.tiji.domain.ai.ValidationResult
import com.bandu.tiji.domain.repository.AiConfigurationRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

@Singleton
class PersistentAiConfigurationRepository @Inject constructor(
    private val portablePreferences: PortablePreferencesStore,
    private val devicePreferences: DevicePreferencesStore,
    private val apiKeyStore: ApiKeyCipherStore,
    private val validationGateway: AiConfigurationValidationGateway,
) : AiConfigurationRepository {
    private val hasApiKey = MutableStateFlow(apiKeyStore.hasKey())

    override fun observeActiveConfiguration(): Flow<AiConfiguration?> =
        combine(portablePreferences.data, hasApiKey) { preferences, keyPresent ->
            preferences.toActiveConfiguration(keyPresent)
        }

    override suspend fun saveAndActivate(draft: AiConfigurationDraft): ValidationResult {
        val oldPortable = portablePreferences.data.first()
        val oldDevice = devicePreferences.data.first()
        val oldKey = apiKeyStore.read()
        val suppliedKey = draft.apiKey
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?.toCharArray()
        val candidateKey = suppliedKey?.copyOf() ?: oldKey?.copyOf()
            ?: return ValidationResult.Failure(listOf("ai.api_key.missing"))

        val validation = try {
            validationGateway.validate(draft, candidateKey)
        } finally {
            candidateKey.fill('\u0000')
        }
        if (validation !is ValidationResult.Success) {
            suppliedKey?.fill('\u0000')
            oldKey?.fill('\u0000')
            return validation
        }

        return try {
            suppliedKey?.let { apiKeyStore.save(it) }
            portablePreferences.replace(oldPortable.withConfiguration(draft))
            devicePreferences.replace(
                oldDevice.copy(allowPrivateHttp = draft.allowPrivateCleartext),
            )
            hasApiKey.value = true
            ValidationResult.Success
        } catch (error: Throwable) {
            restore(
                portable = oldPortable,
                device = oldDevice,
                apiKey = oldKey,
            )
            throw error
        } finally {
            suppliedKey?.fill('\u0000')
            oldKey?.fill('\u0000')
        }
    }

    override suspend fun clearApiKey() {
        apiKeyStore.clear()
        hasApiKey.value = false
    }

    override suspend fun savePrompt(type: PromptType, template: String) {
        val current = portablePreferences.data.first()
        portablePreferences.replace(current.withPrompt(type, template))
    }

    override suspend fun resetPrompt(type: PromptType) {
        val current = portablePreferences.data.first()
        portablePreferences.replace(current.withPrompt(type, ""))
    }

    private suspend fun restore(
        portable: PortablePreferences,
        device: DevicePreferences,
        apiKey: CharArray?,
    ) {
        if (apiKey == null) {
            apiKeyStore.clear()
        } else {
            apiKeyStore.save(apiKey.copyOf())
        }
        portablePreferences.replace(portable)
        devicePreferences.replace(device)
        hasApiKey.value = apiKey != null
    }
}

private fun ApiKeyCipherStore.hasKey(): Boolean =
    read()?.let { apiKey ->
        apiKey.fill('\u0000')
        true
    } ?: false

private fun PortablePreferences.toActiveConfiguration(hasApiKey: Boolean): AiConfiguration? {
    val provider = providerType ?: return null
    if (
        providerDisplayName.isBlank() ||
        baseUrl.isBlank() ||
        analysisModel.isBlank() ||
        tutorModel.isBlank()
    ) {
        return null
    }
    return AiConfiguration(
        id = "configuration-${provider.name.lowercase()}",
        displayName = providerDisplayName,
        providerType = provider,
        baseUrl = baseUrl,
        analysisModel = analysisModel,
        tutorModel = tutorModel,
        hasApiKey = hasApiKey,
    )
}

private fun PortablePreferences.withConfiguration(
    draft: AiConfigurationDraft,
): PortablePreferences =
    copy(
        providerType = draft.providerType,
        providerDisplayName = draft.displayName,
        baseUrl = draft.baseUrl,
        analysisModel = draft.analysisModel,
        tutorModel = draft.tutorModel,
    )

private fun PortablePreferences.withPrompt(
    type: PromptType,
    template: String,
): PortablePreferences =
    when (type) {
        PromptType.ANALYZE_IMAGE -> copy(analyzeImagePrompt = template)
        PromptType.TUTOR -> copy(tutorPrompt = template)
        PromptType.GENERATE_EXERCISE -> copy(generateExercisePrompt = template)
        PromptType.GRADE_EXERCISE -> copy(gradeExercisePrompt = template)
    }
