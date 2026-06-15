package com.bandu.tiji.ai.api.config

import com.bandu.tiji.core.model.enums.AiProviderType

data class ResolvedAiConfiguration(
    val id: String,
    val displayName: String,
    val providerType: AiProviderType,
    val baseUrl: String,
    val apiKey: String,
    val analysisModel: String,
    val tutorModel: String,
    val allowPrivateCleartext: Boolean = false,
) {
    init {
        require(id.isNotBlank()) { "id must not be blank" }
        require(displayName.isNotBlank()) { "displayName must not be blank" }
        require(baseUrl.isNotBlank()) { "baseUrl must not be blank" }
        require(apiKey.isNotBlank()) { "apiKey must not be blank" }
        require(analysisModel.isNotBlank()) { "analysisModel must not be blank" }
        require(tutorModel.isNotBlank()) { "tutorModel must not be blank" }
    }
}

sealed interface ProviderValidation {
    data object Success : ProviderValidation

    data class Failure(val message: String) : ProviderValidation
}
