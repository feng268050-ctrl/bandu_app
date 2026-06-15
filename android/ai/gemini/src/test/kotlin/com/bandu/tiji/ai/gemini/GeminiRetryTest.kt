package com.bandu.tiji.ai.gemini

import com.bandu.tiji.ai.api.config.ResolvedAiConfiguration
import com.bandu.tiji.ai.api.error.AiError
import com.bandu.tiji.ai.api.model.AiStreamEvent
import com.bandu.tiji.ai.api.model.AnalyzeImageRequest
import com.bandu.tiji.ai.api.model.TutorRequest
import com.bandu.tiji.core.model.enums.AiProviderType
import com.bandu.tiji.core.model.id.TutorSessionId
import com.bandu.tiji.core.network.HttpEngine
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Test

class GeminiRetryTest {
    @Test
    fun `image analysis retries one server failure`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(503))
            server.enqueue(MockResponse().setBody(geminiResponse(validAnalysis())))
            val provider = provider()

            val result = provider.analyzeImage(
                configuration(server),
                AnalyzeImageRequest(byteArrayOf(1)),
            )

            assertThat(result.questionText).isEqualTo("题目")
            assertThat(server.requestCount).isEqualTo(2)
        }
    }

    @Test
    fun `authentication failure is not retried`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(401))
            server.enqueue(MockResponse().setBody(geminiResponse(validAnalysis())))
            val provider = provider()

            val failure = runCatching {
                provider.analyzeImage(
                    configuration(server),
                    AnalyzeImageRequest(byteArrayOf(1)),
                )
            }.exceptionOrNull()

            assertThat(failure).isEqualTo(AiError.Authentication)
            assertThat(server.requestCount).isEqualTo(1)
        }
    }

    @Test
    fun `stream retries server failure before first delta`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(503))
            server.enqueue(
                MockResponse().setBody(
                    """
                    data: ${geminiResponse("讲解")}

                    data: [DONE]

                    """.trimIndent(),
                ),
            )
            val events = mutableListOf<AiStreamEvent>()

            provider().streamTutor(
                configuration(server),
                TutorRequest(
                    sessionId = TutorSessionId("session"),
                    userMessage = "讲解",
                    questionContext = "",
                    conversationContext = "",
                ),
            ).collect(events::add)

            assertThat(events).containsExactly(
                AiStreamEvent.Delta("讲解"),
                AiStreamEvent.Completed,
            ).inOrder()
            assertThat(server.requestCount).isEqualTo(2)
        }
    }

    @Test
    fun `stream does not retry after a delta was emitted`() = runTest {
        MockWebServer().use { server ->
            val firstEvent = "data: ${geminiResponse("已显示")}\n\n"
            server.enqueue(
                MockResponse()
                    .setBody(firstEvent + "x".repeat(32 * 1024))
                    .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY),
            )
            server.enqueue(
                MockResponse().setBody(
                    "data: ${geminiResponse("不应请求")}\n\ndata: [DONE]\n\n",
                ),
            )
            val events = mutableListOf<AiStreamEvent>()

            val failure = runCatching {
                provider().streamTutor(
                    configuration(server),
                    TutorRequest(
                        sessionId = TutorSessionId("session"),
                        userMessage = "讲解",
                        questionContext = "",
                        conversationContext = "",
                    ),
                ).collect(events::add)
            }.exceptionOrNull()

            assertThat(events).contains(AiStreamEvent.Delta("已显示"))
            assertThat(failure).isEqualTo(AiError.NetworkUnavailable)
            assertThat(server.requestCount).isEqualTo(1)
        }
    }

    private fun provider() = GeminiAiProvider(
        httpEngineFactory = GeminiHttpEngineFactory { _, _ ->
            HttpEngine(OkHttpClient())
        },
        ioDispatcher = Dispatchers.Unconfined,
    )

    private fun configuration(server: MockWebServer) = ResolvedAiConfiguration(
        id = "gemini",
        displayName = "Gemini",
        providerType = AiProviderType.GEMINI,
        baseUrl = server.url("/").toString().removeSuffix("/"),
        apiKey = "test-secret",
        analysisModel = "gemini-analysis",
        tutorModel = "gemini-tutor",
    )

    private fun validAnalysis(): String = """
        <subject>数学</subject>
        <knowledge_points>函数</knowledge_points>
        <requires_image>false</requires_image>
        <wrong_answer_text></wrong_answer_text>
        <mistake_status>unknown</mistake_status>
        <mistake_analysis></mistake_analysis>
        <question_text>题目</question_text>
        <answer_text>答案</answer_text>
        <analysis>解析</analysis>
    """.trimIndent()

    private fun geminiResponse(text: String): String = buildJsonObject {
        putJsonArray("candidates") {
            add(
                buildJsonObject {
                    putJsonObject("content") {
                        putJsonArray("parts") {
                            add(buildJsonObject { put("text", text) })
                        }
                    }
                },
            )
        }
    }.toString()
}
