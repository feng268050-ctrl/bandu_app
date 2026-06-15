package com.bandu.tiji.ai.api.provider

import com.bandu.tiji.core.model.enums.AiProviderType

class AiProviderRegistry(
    providers: List<AiProvider>,
) {
    private val providersByType: Map<AiProviderType, AiProvider> =
        providers.associateBy { it.type }

    init {
        require(providersByType.size == providers.size) {
            "Duplicate AiProvider registrations are not allowed"
        }
    }

    fun get(type: AiProviderType): AiProvider? = providersByType[type]

    fun require(type: AiProviderType): AiProvider =
        get(type) ?: error("No AiProvider registered for $type")

    fun registeredTypes(): Set<AiProviderType> = providersByType.keys
}
