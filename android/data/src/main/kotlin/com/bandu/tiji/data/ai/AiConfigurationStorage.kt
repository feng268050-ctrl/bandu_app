package com.bandu.tiji.data.ai

import com.bandu.tiji.core.storage.keystore.ApiKeyStore
import com.bandu.tiji.domain.ai.AiConfigurationDraft
import com.bandu.tiji.domain.ai.ValidationResult

interface ApiKeyCipherStore {
    fun read(): CharArray?

    fun save(apiKey: CharArray)

    fun clear()
}

class AndroidApiKeyCipherStore(
    private val delegate: ApiKeyStore,
) : ApiKeyCipherStore {
    override fun read(): CharArray? = delegate.read()

    override fun save(apiKey: CharArray) {
        delegate.save(apiKey)
    }

    override fun clear() {
        delegate.clear()
    }
}

fun interface AiConfigurationValidationGateway {
    suspend fun validate(
        draft: AiConfigurationDraft,
        apiKey: CharArray,
    ): ValidationResult
}
