package com.bandu.tiji.ai.openai

import com.bandu.tiji.ai.api.config.ProviderValidation
import com.bandu.tiji.ai.api.config.ResolvedAiConfiguration
import com.bandu.tiji.ai.api.provider.AiProvider
import com.bandu.tiji.core.model.enums.AiProviderType
import com.bandu.tiji.core.network.AiHttpOperation
import com.bandu.tiji.core.network.HttpEngine
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
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

    @Test
    fun `validation sends minimal chat completions request`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"choices":[]}"""))
            var selectedOperation: AiHttpOperation? = null
            val provider = OpenAiCompatibleProvider(
                httpEngineFactory = OpenAiHttpEngineFactory { _, operation ->
                    selectedOperation = operation
                    HttpEngine(OkHttpClient())
                },
                ioDispatcher = StandardTestDispatcher(testScheduler),
            )

            val result = provider.validate(configuration(server))

            assertThat(result).isEqualTo(ProviderValidation.Success)
            assertThat(selectedOperation).isEqualTo(AiHttpOperation.CONFIGURATION_VALIDATION)
            val request = server.takeRequest()
            assertThat(request.method).isEqualTo("POST")
            assertThat(request.path).isEqualTo("/v1/chat/completions")
            assertThat(request.getHeader("Authorization")).isEqualTo("Bearer test-secret")
            assertThat(request.path).doesNotContain("test-secret")
            val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
            assertThat(body["model"]!!.jsonPrimitive.content).isEqualTo("vision-model")
            assertThat(body["max_tokens"]!!.jsonPrimitive.content).isEqualTo("1")
            assertThat(body["stream"]!!.jsonPrimitive.content).isEqualTo("false")
            assertThat(
                body["messages"]!!.jsonArray[0].jsonObject["content"]!!.jsonPrimitive.content,
            ).isEqualTo("ping")
        }
    }

    @Test
    fun `validation failure does not expose provider response body`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setResponseCode(401)
                    .setBody("secret-provider-error"),
            )
            val provider = provider()

            val result = provider.validate(configuration(server))

            assertThat(result).isEqualTo(ProviderValidation.Failure("authentication"))
            assertThat(result.toString()).doesNotContain("secret-provider-error")
        }
    }

    private fun provider(): OpenAiCompatibleProvider = OpenAiCompatibleProvider(
        httpEngineFactory = OpenAiHttpEngineFactory { _, _ ->
            HttpEngine(OkHttpClient())
        },
        ioDispatcher = Dispatchers.Unconfined,
    )

    private fun configuration(server: MockWebServer): ResolvedAiConfiguration =
        ResolvedAiConfiguration(
            id = "openai",
            displayName = "OpenAI compatible",
            providerType = AiProviderType.OPENAI_COMPATIBLE,
            baseUrl = server.url("/v1").toString().removeSuffix("/"),
            apiKey = "test-secret",
            analysisModel = "vision-model",
            tutorModel = "tutor-model",
        )
}
