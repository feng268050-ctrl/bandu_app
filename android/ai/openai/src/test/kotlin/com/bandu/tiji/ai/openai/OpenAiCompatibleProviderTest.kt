package com.bandu.tiji.ai.openai

import com.bandu.tiji.ai.api.config.ProviderValidation
import com.bandu.tiji.ai.api.config.ResolvedAiConfiguration
import com.bandu.tiji.ai.api.error.AiError
import com.bandu.tiji.ai.api.model.AnalyzeImageRequest
import com.bandu.tiji.ai.api.model.AiStreamEvent
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
    fun `validation sends image chat completions request`() = runTest {
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
            val content = body["messages"]!!
                .jsonArray[0]
                .jsonObject["content"]!!
                .jsonArray
            assertThat(content[0].jsonObject["type"]!!.jsonPrimitive.content).isEqualTo("text")
            assertThat(content[0].jsonObject["text"]!!.jsonPrimitive.content)
                .isEqualTo("Reply with ok.")
            assertThat(content[1].jsonObject["type"]!!.jsonPrimitive.content)
                .isEqualTo("image_url")
            assertThat(
                content[1]
                    .jsonObject["image_url"]!!
                    .jsonObject["url"]!!
                    .jsonPrimitive.content,
            ).startsWith("data:image/png;base64,")
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

    @Test
    fun `non streaming completion sends text and extracts standard content`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse().setBody(
                    """
                    {
                      "id": "chatcmpl-test",
                      "choices": [
                        {
                          "index": 0,
                          "message": {
                            "role": "assistant",
                            "content": "完整回答"
                          },
                          "finish_reason": "stop"
                        }
                      ],
                      "usage": {"prompt_tokens": 3, "completion_tokens": 2}
                    }
                    """.trimIndent(),
                ),
            )

            val result = provider().generateText(
                configuration = configuration(server),
                model = "tutor-model",
                prompt = "请讲解",
                operation = AiHttpOperation.EXERCISE,
            )

            assertThat(result).isEqualTo("完整回答")
            val request = server.takeRequest()
            assertThat(request.path).isEqualTo("/v1/chat/completions")
            val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
            assertThat(body["model"]!!.jsonPrimitive.content).isEqualTo("tutor-model")
            assertThat(body["stream"]!!.jsonPrimitive.content).isEqualTo("false")
            assertThat(
                body["messages"]!!.jsonArray[0].jsonObject["content"]!!.jsonPrimitive.content,
            ).isEqualTo("请讲解")
        }
    }

    @Test
    fun `completion accepts response without optional metadata`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse().setBody(
                    """{"choices":[{"message":{"content":"兼容回答"}}]}""",
                ),
            )

            val result = provider().generateText(
                configuration(server),
                "tutor-model",
                "问题",
                AiHttpOperation.EXERCISE,
            )

            assertThat(result).isEqualTo("兼容回答")
        }
    }

    @Test
    fun `completion joins text parts returned by compatible service`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse().setBody(
                    """
                    {"choices":[{"message":{"content":[
                      {"type":"text","text":"第一段"},
                      {"type":"text","text":"第二段"}
                    ]}}]}
                    """.trimIndent(),
                ),
            )

            val result = provider().generateText(
                configuration(server),
                "tutor-model",
                "问题",
                AiHttpOperation.EXERCISE,
            )

            assertThat(result).isEqualTo("第一段第二段")
        }
    }

    @Test
    fun `completion rejects missing content`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"choices":[{"message":{}}]}"""))

            val error = runCatching {
                provider().generateText(
                    configuration(server),
                    "tutor-model",
                    "问题",
                    AiHttpOperation.EXERCISE,
                )
            }.exceptionOrNull()

            assertThat(error).isEqualTo(AiError.InvalidResponse("openai.empty_response"))
        }
    }

    @Test
    fun `image analysis sends text and image_url data URL parts`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse().setBody(
                    """{"choices":[{"message":{"content":"分析结果"}}]}""",
                ),
            )
            val imageBytes = byteArrayOf(0x01, 0x02, 0x7f)

            val result = provider().generateImageAnalysisText(
                configuration = configuration(server),
                request = AnalyzeImageRequest(
                    imageBytes = imageBytes,
                    mimeType = "image/jpeg",
                ),
                prompt = "分析这道题",
            )

            assertThat(result).isEqualTo("分析结果")
            val recorded = server.takeRequest()
            val root = Json.parseToJsonElement(recorded.body.readUtf8()).jsonObject
            assertThat(root["model"]!!.jsonPrimitive.content).isEqualTo("vision-model")
            val parts = root["messages"]!!.jsonArray[0]
                .jsonObject["content"]!!.jsonArray
            assertThat(parts[0].jsonObject["type"]!!.jsonPrimitive.content).isEqualTo("text")
            assertThat(parts[0].jsonObject["text"]!!.jsonPrimitive.content)
                .isEqualTo("分析这道题")
            assertThat(parts[1].jsonObject["type"]!!.jsonPrimitive.content)
                .isEqualTo("image_url")
            assertThat(
                parts[1].jsonObject["image_url"]!!.jsonObject["url"]!!.jsonPrimitive.content,
            ).isEqualTo("data:image/jpeg;base64,AQJ/")
            assertThat(recorded.getHeader("Authorization")).isEqualTo("Bearer test-secret")
        }
    }

    @Test
    fun `stream parses UTF-8 chunks and completes at done`() = runTest {
        MockWebServer().use { server ->
            val body = """
                data: {"choices":[{"delta":{"content":"你"}}]}

                data: {"choices":[{"delta":{"content":"好"}}]}

                data: [DONE]

            """.trimIndent()
            server.enqueue(
                MockResponse()
                    .addHeader("Content-Type", "text/event-stream; charset=utf-8")
                    .setBody(body)
                    .throttleBody(1, 1, TimeUnit.MILLISECONDS),
            )

            val events = provider().streamText(
                configuration(server),
                "tutor-model",
                "开始辅导",
            ).toList()

            assertThat(events).containsExactly(
                AiStreamEvent.Delta("你"),
                AiStreamEvent.Delta("好"),
                AiStreamEvent.Completed,
            ).inOrder()
            val request = server.takeRequest()
            assertThat(request.path).isEqualTo("/v1/chat/completions")
            val root = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
            assertThat(root["stream"]!!.jsonPrimitive.content).isEqualTo("true")
            assertThat(root["stream_options"]!!.jsonObject["include_usage"]!!.jsonPrimitive.content)
                .isEqualTo("true")
        }
    }

    @Test
    fun `stream completes at normal EOF without done marker`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse().setBody(
                    """
                    data: {"choices":[{"delta":{"content":"完成"}}]}

                    """.trimIndent(),
                ),
            )

            val events = provider().streamText(
                configuration(server),
                "tutor-model",
                "开始",
            ).toList()

            assertThat(events).containsExactly(
                AiStreamEvent.Delta("完成"),
                AiStreamEvent.Completed,
            ).inOrder()
        }
    }

    @Test
    fun `stream ignores role only and usage only compatible events`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse().setBody(
                    """
                    data: {"choices":[{"delta":{"role":"assistant"}}]}

                    data: {"choices":[],"usage":{"prompt_tokens":1,"completion_tokens":2}}

                    data: [DONE]

                    """.trimIndent(),
                ),
            )

            val events = provider().streamText(
                configuration(server),
                "tutor-model",
                "开始",
            ).toList()

            assertThat(events).containsExactly(AiStreamEvent.Completed)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `cancelling stream cancels blocked HTTP call`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            val provider = OpenAiCompatibleProvider(
                httpEngineFactory = OpenAiHttpEngineFactory { _, _ ->
                    HttpEngine(OkHttpClient())
                },
                ioDispatcher = Dispatchers.IO,
            )
            val collectJob = launch {
                provider.streamText(
                    configuration(server),
                    "tutor-model",
                    "等待",
                ).toList()
            }
            runCurrent()
            assertThat(server.takeRequest(5, TimeUnit.SECONDS)).isNotNull()

            collectJob.cancelAndJoin()

            assertThat(collectJob.isCancelled).isTrue()
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
