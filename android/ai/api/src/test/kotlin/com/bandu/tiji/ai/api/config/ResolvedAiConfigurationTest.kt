package com.bandu.tiji.ai.api.config

import com.bandu.tiji.core.model.enums.AiProviderType
import org.junit.Assert.assertThrows
import org.junit.Test

class ResolvedAiConfigurationTest {
    @Test
    fun `requires non blank configuration fields`() {
        ResolvedAiConfiguration(
            id = "cfg-1",
            displayName = "Gemini",
            providerType = AiProviderType.GEMINI,
            baseUrl = "https://generativelanguage.googleapis.com",
            apiKey = "secret",
            analysisModel = "gemini-pro-vision",
            tutorModel = "gemini-pro",
        )
    }

    @Test
    fun `rejects blank api key`() {
        assertThrows(IllegalArgumentException::class.java) {
            ResolvedAiConfiguration(
                id = "cfg-1",
                displayName = "Gemini",
                providerType = AiProviderType.GEMINI,
                baseUrl = "https://example.com",
                apiKey = "",
                analysisModel = "model-a",
                tutorModel = "model-b",
            )
        }
    }
}
