package com.bandu.tiji.data.ai

import app.cash.turbine.test
import com.bandu.tiji.ai.api.config.ProviderValidation
import com.bandu.tiji.ai.api.config.ResolvedAiConfiguration
import com.bandu.tiji.ai.api.error.AiError
import com.bandu.tiji.ai.api.model.AiStreamEvent
import com.bandu.tiji.ai.api.model.AnalyzeImageRequest
import com.bandu.tiji.ai.api.model.AnalyzedQuestion
import com.bandu.tiji.ai.api.model.ExerciseGrade
import com.bandu.tiji.ai.api.model.ExerciseRequest
import com.bandu.tiji.ai.api.model.GeneratedExercise
import com.bandu.tiji.ai.api.model.GradeExerciseRequest
import com.bandu.tiji.ai.api.model.TutorRequest
import com.bandu.tiji.ai.api.provider.AiProvider
import com.bandu.tiji.ai.api.provider.AiProviderRegistry
import com.bandu.tiji.core.model.enums.AiProviderType
import com.bandu.tiji.core.model.enums.GradeResult
import com.bandu.tiji.core.model.enums.MistakeStatus
import com.bandu.tiji.core.model.id.ExerciseId
import com.bandu.tiji.core.model.id.TutorSessionId
import com.bandu.tiji.domain.ai.AiGatewayException
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ProviderAiGatewayTest {
    @Test
    fun `gateway routes every operation to active provider and maps models`() = runTest {
        val gemini = RecordingProvider(AiProviderType.GEMINI)
        val openAi = RecordingProvider(AiProviderType.OPENAI_COMPATIBLE)
        val gateway = ProviderAiTutorGateway(
            AiProviderRegistry(listOf(gemini, openAi)),
            ActiveAiConfigurationResolver { configuration(AiProviderType.OPENAI_COMPATIBLE) },
        )

        val analyzed = gateway.analyzeImage(
            com.bandu.tiji.domain.ai.AnalyzeImageRequest(byteArrayOf(1)),
        )
        assertThat(analyzed.questionText).isEqualTo("question")

        gateway.streamTutor(
            com.bandu.tiji.domain.ai.TutorRequest(
                TutorSessionId("session-1"),
                "help",
                "question",
                "conversation",
            ),
        ).test {
            assertThat(awaitItem()).isEqualTo(com.bandu.tiji.domain.ai.AiStreamEvent.Delta("delta"))
            assertThat(awaitItem()).isEqualTo(com.bandu.tiji.domain.ai.AiStreamEvent.Completed)
            awaitComplete()
        }

        val generated = gateway.generateExercise(
            com.bandu.tiji.domain.ai.ExerciseRequest(
                TutorSessionId("session-1"),
                "question",
                "algebra",
                com.bandu.tiji.core.model.enums.ExerciseDifficulty.MEDIUM,
            ),
        )
        val grade = gateway.gradeExercise(
            com.bandu.tiji.domain.ai.GradeExerciseRequest(
                ExerciseId("exercise-1"),
                "question",
                "answer",
                "answer",
            ),
        )

        assertThat(generated.questionText).isEqualTo("generated")
        assertThat(grade.result).isEqualTo(GradeResult.CORRECT)
        assertThat(openAi.calls).containsExactly("analyze", "stream", "generate", "grade").inOrder()
        assertThat(gemini.calls).isEmpty()
    }

    @Test
    fun `gateway normalizes provider and configuration errors`() = runTest {
        val provider = RecordingProvider(AiProviderType.GEMINI).apply {
            analyzeFailure = AiError.Authentication
        }
        val gateway = ProviderAiTutorGateway(
            AiProviderRegistry(listOf(provider)),
            ActiveAiConfigurationResolver { configuration(AiProviderType.GEMINI) },
        )

        val authError = runCatching {
            gateway.analyzeImage(com.bandu.tiji.domain.ai.AnalyzeImageRequest(byteArrayOf(1)))
        }.exceptionOrNull()
        assertThat(authError).isEqualTo(AiGatewayException.Authentication)

        val missingGateway = ProviderAiTutorGateway(
            AiProviderRegistry(listOf(provider)),
            ActiveAiConfigurationResolver { throw AiGatewayException.ConfigurationRequired },
        )
        val missingError = runCatching {
            missingGateway.generateExercise(
                com.bandu.tiji.domain.ai.ExerciseRequest(
                    TutorSessionId("session-1"),
                    "question",
                    "algebra",
                    com.bandu.tiji.core.model.enums.ExerciseDifficulty.EASY,
                ),
            )
        }.exceptionOrNull()
        assertThat(missingError).isEqualTo(AiGatewayException.ConfigurationRequired)
    }

    private fun configuration(type: AiProviderType) =
        ResolvedAiConfiguration(
            id = "config",
            displayName = "Provider",
            providerType = type,
            baseUrl = "https://example.com/v1",
            apiKey = "secret",
            analysisModel = "analysis",
            tutorModel = "tutor",
        )

    private class RecordingProvider(
        override val type: AiProviderType,
    ) : AiProvider {
        val calls = mutableListOf<String>()
        var analyzeFailure: Throwable? = null

        override suspend fun validate(configuration: ResolvedAiConfiguration): ProviderValidation =
            ProviderValidation.Success

        override suspend fun analyzeImage(
            configuration: ResolvedAiConfiguration,
            request: AnalyzeImageRequest,
        ): AnalyzedQuestion {
            calls += "analyze"
            analyzeFailure?.let { throw it }
            return AnalyzedQuestion(
                subject = "数学",
                knowledgePoints = listOf("代数"),
                requiresImage = false,
                wrongAnswerText = "",
                mistakeStatus = MistakeStatus.UNKNOWN,
                mistakeAnalysis = "",
                questionText = "question",
                answerText = "answer",
                analysis = "analysis",
            )
        }

        override fun streamTutor(
            configuration: ResolvedAiConfiguration,
            request: TutorRequest,
        ): Flow<AiStreamEvent> {
            calls += "stream"
            return flowOf(AiStreamEvent.Delta("delta"), AiStreamEvent.Completed)
        }

        override suspend fun generateExercise(
            configuration: ResolvedAiConfiguration,
            request: ExerciseRequest,
        ): GeneratedExercise {
            calls += "generate"
            return GeneratedExercise("generated", "answer", "analysis")
        }

        override suspend fun gradeExercise(
            configuration: ResolvedAiConfiguration,
            request: GradeExerciseRequest,
        ): ExerciseGrade {
            calls += "grade"
            return ExerciseGrade(
                result = GradeResult.CORRECT,
                feedback = "good",
                confidence = 0.9,
                rawResult = GradeResult.CORRECT,
            )
        }
    }
}
