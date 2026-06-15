package com.bandu.tiji.ai.openai

import com.bandu.tiji.ai.api.provider.AiProvider
import com.bandu.tiji.core.model.enums.AiProviderType
import com.bandu.tiji.core.network.HttpEngine
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import org.junit.Test

class OpenAiCompatibleProviderTest {
    @Test
    fun `provider exposes OpenAI compatible type through common contract`() {
        val provider: AiProvider = OpenAiCompatibleProvider()

        assertThat(provider.type).isEqualTo(AiProviderType.OPENAI_COMPATIBLE)
    }

    @Test
    fun `provider accepts isolated HTTP and dispatcher dependencies`() {
        val provider: AiProvider = OpenAiCompatibleProvider(
            httpEngineFactory = OpenAiHttpEngineFactory { _, _ ->
                HttpEngine(OkHttpClient())
            },
            ioDispatcher = Dispatchers.Unconfined,
        )

        assertThat(provider.type).isEqualTo(AiProviderType.OPENAI_COMPATIBLE)
    }
}
