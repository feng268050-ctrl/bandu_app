package com.bandu.tiji.ai.openai

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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test

class OpenAiPromptIntegrationTest {
    @Test
    fun `default templates satisfy all prompt contracts`() {
        val renderer = PromptRenderer()

        PromptType.ALL.forEach { type ->
            assertThat(renderer.validate(type, DefaultOpenAiPromptTemplates.template(type)))
                .isInstanceOf(PromptValidationResult.Valid::class.java)
        }
    }

    @Test
    fun `four public operations render prompts and select required models`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody(completion(analysisText())))
            server.enqueue(MockResponse().setBody(tutorStream()))
            server.enqueue(MockResponse().setBody(completion(exerciseText())))
            server.enqueue(MockResponse().setBody(completion(gradeText())))
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
            assertModel(analysisRequest.body.readUtf8(), "vision-model")
            assertModel(tutorRequest.body.readUtf8(), "tutor-model")
            assertModel(exerciseRequest.body.readUtf8(), "tutor-model")
            assertModel(gradeRequest.body.readUtf8(), "tutor-model")
        }
    }

    private fun assertModel(body: String, expected: String) {
        val model = Json.parseToJsonElement(body).jsonObject["model"]!!.jsonPrimitive.content
        assertThat(model).isEqualTo(expected)
    }

    private fun provider() = OpenAiCompatibleProvider(
        httpEngineFactory = OpenAiHttpEngineFactory { _, _ ->
            HttpEngine(OkHttpClient())
        },
        ioDispatcher = Dispatchers.Unconfined,
    )

    private fun configuration(server: MockWebServer) = ResolvedAiConfiguration(
        id = "openai",
        displayName = "OpenAI compatible",
        providerType = AiProviderType.OPENAI_COMPATIBLE,
        baseUrl = server.url("/v1").toString().removeSuffix("/"),
        apiKey = "test-secret",
        analysisModel = "vision-model",
        tutorModel = "tutor-model",
    )

    private fun completion(text: String): String =
        """{"choices":[{"message":{"content":${JsonPrimitive(text)}}}]}"""

    private fun tutorStream(): String = """
        data: {"choices":[{"delta":{"content":"讲解"}}]}

        data: [DONE]

    """.trimIndent()

    private fun analysisText(): String = """
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

    private fun exerciseText(): String = """
        <question_text>变式题</question_text>
        <answer_text>42</answer_text>
        <analysis>解析</analysis>
    """.trimIndent()

    private fun gradeText(): String = """
        <grade>correct</grade>
        <feedback>正确</feedback>
        <confidence>0.9</confidence>
    """.trimIndent()
}
