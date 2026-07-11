package com.bandu.tiji.ai.gemini

import com.bandu.tiji.ai.api.config.ResolvedAiConfiguration
import com.bandu.tiji.ai.api.model.AnalyzeImageRequest
import com.bandu.tiji.ai.api.model.ExerciseRequest
import com.bandu.tiji.ai.api.model.GradeExerciseRequest
import com.bandu.tiji.ai.api.model.TutorRequest
import com.bandu.tiji.ai.api.prompt.PromptRenderer
import com.bandu.tiji.ai.api.prompt.PromptType
import com.bandu.tiji.ai.api.prompt.PromptValidationResult
import com.bandu.tiji.core.model.enums.AiProviderType
import com.bandu.tiji.core.model.enums.ExerciseDifficulty
import com.bandu.tiji.core.model.enums.GradeResult
import com.bandu.tiji.core.model.id.ExerciseId
import com.bandu.tiji.core.model.id.TutorSessionId
import com.bandu.tiji.core.network.HttpEngine
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test

class GeminiPromptIntegrationTest {
    @Test
    fun `default templates satisfy all prompt contracts`() {
        val renderer = PromptRenderer()

        PromptType.ALL.forEach { type ->
            assertThat(renderer.validate(type, DefaultGeminiPromptTemplates.template(type)))
                .isInstanceOf(PromptValidationResult.Valid::class.java)
        }
    }

    @Test
    fun `split question bank prompt uses question numbers as boundaries`() {
        val template = DefaultGeminiPromptTemplates.template(PromptType.SPLIT_QUESTION_BANK_PAGE)

        assertThat(template).contains("题号是拆题边界")
        assertThat(template).contains("同一个 <question> 内不能包含两个或更多题号")
        assertThat(template).contains("将题号保留在 <stem> 开头")
    }

    @Test
    fun `four public operations render prompts and select required models`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody(analysisResponse()))
            server.enqueue(MockResponse().setBody(tutorStreamResponse()))
            server.enqueue(MockResponse().setBody(exerciseResponse()))
            server.enqueue(MockResponse().setBody(gradeResponse()))
            val provider = provider()
            val configuration = configuration(server)

            val analysis = provider.analyzeImage(
                configuration,
                AnalyzeImageRequest(
                    imageBytes = byteArrayOf(1),
                    gradeInstruction = "初二",
                    knowledgePointsList = "函数",
                    languageInstruction = "简体中文",
                    providerHints = "严格格式",
                ),
            )
            val tutorEvents = provider.streamTutor(
                configuration,
                TutorRequest(
                    sessionId = TutorSessionId("session"),
                    userMessage = "怎么做",
                    questionContext = "原题",
                    conversationContext = "历史",
                    gradeInstruction = "初二",
                ),
            ).toList()
            val exercise = provider.generateExercise(
                configuration,
                ExerciseRequest(
                    sessionId = TutorSessionId("session"),
                    originalQuestion = "原题",
                    knowledgePoints = "函数",
                    difficulty = ExerciseDifficulty.HARD,
                    gradeInstruction = "初二",
                ),
            )
            val grade = provider.gradeExercise(
                configuration,
                GradeExerciseRequest(
                    exerciseId = ExerciseId("exercise"),
                    exerciseQuestion = "新题",
                    expectedAnswer = "42",
                    userAnswer = "42",
                    rubricContext = "步骤完整",
                ),
            )

            assertThat(analysis.questionText).isEqualTo("题目")
            assertThat(tutorEvents).isNotEmpty()
            assertThat(exercise.questionText).isEqualTo("变式题")
            assertThat(grade.result).isEqualTo(GradeResult.CORRECT)

            val analysisRequest = server.takeRequest()
            val tutorRequest = server.takeRequest()
            val exerciseRequest = server.takeRequest()
            val gradeRequest = server.takeRequest()
            assertThat(analysisRequest.path).contains("/gemini-analysis:generateContent")
            assertThat(tutorRequest.path).contains("/gemini-tutor:streamGenerateContent")
            assertThat(exerciseRequest.path).contains("/gemini-tutor:generateContent")
            assertThat(gradeRequest.path).contains("/gemini-tutor:generateContent")
            assertThat(analysisRequest.body.readUtf8()).contains("初二")
            assertThat(tutorRequest.body.readUtf8()).contains("怎么做")
            assertThat(exerciseRequest.body.readUtf8()).contains("hard")
            assertThat(gradeRequest.body.readUtf8()).contains("步骤完整")
        }
    }

    private fun provider(): GeminiAiProvider = GeminiAiProvider(
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

    private fun analysisResponse(): String = geminiResponse(
        """
        <subject>数学</subject>
        <knowledge_points>函数</knowledge_points>
        <requires_image>false</requires_image>
        <wrong_answer_text></wrong_answer_text>
        <mistake_status>unknown</mistake_status>
        <mistake_analysis></mistake_analysis>
        <question_text>题目</question_text>
        <answer_text>答案</answer_text>
        <analysis>解析</analysis>
        """.trimIndent(),
    )

    private fun tutorStreamResponse(): String = """
        data: ${geminiResponse("讲解")}

        data: [DONE]

    """.trimIndent()

    private fun exerciseResponse(): String = geminiResponse(
        """
        <question_text>变式题</question_text>
        <answer_text>42</answer_text>
        <analysis>解析</analysis>
        """.trimIndent(),
    )

    private fun gradeResponse(): String = geminiResponse(
        """
        <grade>correct</grade>
        <feedback>正确</feedback>
        <confidence>0.9</confidence>
        """.trimIndent(),
    )

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
