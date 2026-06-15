package com.bandu.tiji.ai.openai

import com.bandu.tiji.ai.api.config.ResolvedAiConfiguration
import com.bandu.tiji.ai.api.model.AnalyzeImageRequest
import com.bandu.tiji.core.common.logging.DebugAppLogger
import com.bandu.tiji.core.model.enums.AiProviderType
import com.bandu.tiji.core.network.endpoint.DefaultEndpointPolicy
import com.google.common.truth.Truth.assertThat
import java.util.Base64
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test

class OpenAiLogRedactionTest {
    @Test
    fun `network logs exclude key authorization image prompt and response body`() = runTest {
        val apiKey = "openai-compatible-secret-key"
        val sensitivePromptValue = "完整题目正文不得记录"
        val responseBody = "兼容服务错误正文不得记录"
        val imageBytes = byteArrayOf(1, 2, 3, 4, 5, 6)
        val encodedImage = Base64.getEncoder().encodeToString(imageBytes)
        val logged = mutableListOf<String>()
        val logger = DebugAppLogger { level, event, fields, throwable ->
            logged += "$level|$event|$fields|${throwable?.javaClass?.simpleName}"
        }

        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(400).setBody(responseBody))
            val provider = OpenAiCompatibleProvider(
                httpEngineFactory = DefaultOpenAiHttpEngineFactory(
                    endpointPolicy = DefaultEndpointPolicy(),
                    logger = logger,
                ),
            )
            val configuration = ResolvedAiConfiguration(
                id = "openai",
                displayName = "OpenAI compatible",
                providerType = AiProviderType.OPENAI_COMPATIBLE,
                baseUrl = server.url("/v1").toString().removeSuffix("/"),
                apiKey = apiKey,
                analysisModel = "vision-model",
                tutorModel = "tutor-model",
                allowPrivateCleartext = true,
            )

            runCatching {
                provider.analyzeImage(
                    configuration,
                    AnalyzeImageRequest(
                        imageBytes = imageBytes,
                        gradeInstruction = sensitivePromptValue,
                    ),
                )
            }

            val rendered = logged.joinToString("\n")
            assertThat(server.requestCount).isEqualTo(1)
            assertThat(rendered).doesNotContain(apiKey)
            assertThat(rendered).doesNotContain("Bearer $apiKey")
            assertThat(rendered).doesNotContain(encodedImage)
            assertThat(rendered).doesNotContain(sensitivePromptValue)
            assertThat(rendered).doesNotContain(responseBody)
            assertThat(rendered).doesNotContain("Authorization")
            assertThat(rendered).contains("statusCategory=4xx")
        }
    }
}
