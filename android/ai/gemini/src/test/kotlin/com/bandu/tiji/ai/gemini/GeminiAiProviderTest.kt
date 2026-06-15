package com.bandu.tiji.ai.gemini

import com.bandu.tiji.ai.api.config.ProviderValidation
import com.bandu.tiji.ai.api.config.ResolvedAiConfiguration
import com.bandu.tiji.ai.api.error.AiError
import com.bandu.tiji.ai.api.model.AiStreamEvent
import com.bandu.tiji.ai.api.model.AnalyzeImageRequest
import com.bandu.tiji.ai.api.provider.AiProvider
import com.bandu.tiji.core.model.enums.AiProviderType
import com.bandu.tiji.core.network.AiHttpOperation
import com.bandu.tiji.core.network.HttpEngine
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Test

class GeminiAiProviderTest {
    @Test
    fun `provider exposes Gemini type through common contract`() {
        val provider: AiProvider = GeminiAiProvider()

        assertThat(provider.type).isEqualTo(AiProviderType.GEMINI)
    }

    @Test
    fun `validation sends minimal request with key in header`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"candidates":[]}"""))
            var selectedOperation: AiHttpOperation? = null
            val provider = GeminiAiProvider(
                httpEngineFactory = GeminiHttpEngineFactory { _, operation ->
                    selectedOperation = operation
                    HttpEngine(OkHttpClient())
                },
                ioDispatcher = StandardTestDispatcher(testScheduler),
            )
            val configuration = configuration(server)

            val result = provider.validate(configuration)

            assertThat(result).isEqualTo(ProviderValidation.Success)
            assertThat(selectedOperation).isEqualTo(AiHttpOperation.CONFIGURATION_VALIDATION)
            val request = server.takeRequest()
            assertThat(request.method).isEqualTo("POST")
            assertThat(request.path).isEqualTo(
                "/v1beta/models/gemini-analysis:generateContent",
            )
            assertThat(request.getHeader("x-goog-api-key")).isEqualTo("test-secret")
            assertThat(request.path).doesNotContain("test-secret")
            val body = Json.parseToJsonElement(request.body.readUtf8())
            assertThat(body.toString()).contains("\"maxOutputTokens\":1")
            assertThat(body.toString()).contains("\"text\":\"ping\"")
        }
    }

    @Test
    fun `validation returns generic failure without response body`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(401).setBody("secret-provider-error"))
            val provider = provider()

            val result = provider.validate(configuration(server))

            assertThat(result).isEqualTo(ProviderValidation.Failure("authentication"))
            assertThat(result.toString()).doesNotContain("secret-provider-error")
        }
    }

    @Test
    fun `generateContent sends text and joins response parts`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse().setBody(
                    """
                    {
                      "candidates": [
                        {
                          "content": {
                            "parts": [
                              {"text": "第一段"},
                              {"text": "第二段"}
                            ]
                          }
                        }
                      ]
                    }
                    """.trimIndent(),
                ),
            )
            val provider = provider()

            val result = provider.generateText(
                configuration = configuration(server),
                model = "gemini-tutor",
                prompt = "请讲解",
                operation = AiHttpOperation.EXERCISE,
            )

            assertThat(result).isEqualTo("第一段第二段")
            val request = server.takeRequest()
            assertThat(request.path).isEqualTo(
                "/v1beta/models/gemini-tutor:generateContent",
            )
            assertThat(request.body.readUtf8()).contains("\"text\":\"请讲解\"")
            assertThat(request.getHeader("x-goog-api-key")).isEqualTo("test-secret")
        }
    }

    @Test
    fun `generateContent rejects empty candidate text`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"candidates":[{"content":{"parts":[]}}]}"""))
            val provider = provider()

            val error = runCatching {
                provider.generateText(
                    configuration = configuration(server),
                    model = "gemini-tutor",
                    prompt = "请讲解",
                    operation = AiHttpOperation.EXERCISE,
                )
            }.exceptionOrNull()

            assertThat(error).isInstanceOf(AiError.InvalidResponse::class.java)
            assertThat((error as AiError.InvalidResponse).diagnosticCode)
                .isEqualTo("gemini.empty_response")
        }
    }

    @Test
    fun `image request uses analysis model JPEG inline data and base64`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse().setBody(
                    """{"candidates":[{"content":{"parts":[{"text":"分析结果"}]}}]}""",
                ),
            )
            val provider = provider()
            val imageBytes = byteArrayOf(0x01, 0x02, 0x7f)

            val result = provider.generateImageAnalysisText(
                configuration = configuration(server),
                request = AnalyzeImageRequest(imageBytes = imageBytes),
                prompt = "分析这道题",
            )

            assertThat(result).isEqualTo("分析结果")
            val recorded = server.takeRequest()
            assertThat(recorded.path).isEqualTo(
                "/v1beta/models/gemini-analysis:generateContent",
            )
            val root = Json.parseToJsonElement(recorded.body.readUtf8()).jsonObject
            val parts = root["contents"]!!.jsonArray[0]
                .jsonObject["parts"]!!.jsonArray
            assertThat(parts[0].jsonObject["text"]!!.jsonPrimitive.content)
                .isEqualTo("分析这道题")
            val inlineData = parts[1].jsonObject["inline_data"]!!.jsonObject
            assertThat(inlineData["mime_type"]!!.jsonPrimitive.content)
                .isEqualTo("image/jpeg")
            assertThat(inlineData["data"]!!.jsonPrimitive.content).isEqualTo("AQJ/")
        }
    }

    @Test
    fun `image request rejects non JPEG input before network call`() = runTest {
        MockWebServer().use { server ->
            val provider = provider()

            val error = runCatching {
                provider.generateImageAnalysisText(
                    configuration = configuration(server),
                    request = AnalyzeImageRequest(
                        imageBytes = byteArrayOf(1),
                        mimeType = "image/png",
                    ),
                    prompt = "分析",
                )
            }.exceptionOrNull()

            assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
            assertThat(server.requestCount).isEqualTo(0)
        }
    }

    @Test
    fun `stream parses UTF-8 chunks and completes at done`() = runTest {
        MockWebServer().use { server ->
            val body = """
                data: {"candidates":[{"content":{"parts":[{"text":"你"}]}}]}

                data: {"candidates":[{"content":{"parts":[{"text":"好"}]}}]}

                data: [DONE]

            """.trimIndent()
            server.enqueue(
                MockResponse()
                    .addHeader("Content-Type", "text/event-stream; charset=utf-8")
                    .setBody(body)
                    .throttleBody(1, 1, TimeUnit.MILLISECONDS),
            )
            val provider = provider()

            val events = provider.streamText(
                configuration = configuration(server),
                model = "gemini-tutor",
                prompt = "开始辅导",
            ).toList()

            assertThat(events).containsExactly(
                AiStreamEvent.Delta("你"),
                AiStreamEvent.Delta("好"),
                AiStreamEvent.Completed,
            ).inOrder()
            assertThat(server.takeRequest().path).isEqualTo(
                "/v1beta/models/gemini-tutor:streamGenerateContent?alt=sse",
            )
        }
    }

    @Test
    fun `stream completes at normal EOF without done marker`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse().setBody(
                    """
                    data: {"candidates":[{"content":{"parts":[{"text":"完成"}]}}]}

                    """.trimIndent(),
                ),
            )
            val provider = provider()

            val events = provider.streamText(
                configuration(server),
                "gemini-tutor",
                "开始",
            ).toList()

            assertThat(events).containsExactly(
                AiStreamEvent.Delta("完成"),
                AiStreamEvent.Completed,
            ).inOrder()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `cancelling stream cancels blocked HTTP call`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            val provider = GeminiAiProvider(
                httpEngineFactory = GeminiHttpEngineFactory { _, _ ->
                    HttpEngine(OkHttpClient())
                },
                ioDispatcher = Dispatchers.IO,
            )
            val collectJob = launch {
                provider.streamText(
                    configuration(server),
                    "gemini-tutor",
                    "等待",
                ).toList()
            }
            runCurrent()
            assertThat(server.takeRequest(5, TimeUnit.SECONDS)).isNotNull()

            collectJob.cancelAndJoin()

            assertThat(collectJob.isCancelled).isTrue()
        }
    }

    private fun provider(): GeminiAiProvider = GeminiAiProvider(
        httpEngineFactory = GeminiHttpEngineFactory { _, _ ->
            HttpEngine(OkHttpClient())
        },
        ioDispatcher = Dispatchers.Unconfined,
    )

    private fun configuration(server: MockWebServer): ResolvedAiConfiguration =
        ResolvedAiConfiguration(
            id = "gemini",
            displayName = "Gemini",
            providerType = AiProviderType.GEMINI,
            baseUrl = server.url("/").toString().removeSuffix("/"),
            apiKey = "test-secret",
            analysisModel = "gemini-analysis",
            tutorModel = "gemini-tutor",
        )
}
