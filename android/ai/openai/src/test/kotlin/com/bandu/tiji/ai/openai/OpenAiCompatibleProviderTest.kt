package com.bandu.tiji.ai.openai

import com.bandu.tiji.ai.api.provider.AiProvider
import com.bandu.tiji.core.model.enums.AiProviderType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class OpenAiCompatibleProviderTest {
    @Test
    fun `provider exposes OpenAI compatible type through common contract`() {
        val provider: AiProvider = OpenAiCompatibleProvider()

        assertThat(provider.type).isEqualTo(AiProviderType.OPENAI_COMPATIBLE)
    }
}
