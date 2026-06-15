package com.bandu.tiji.ai.api.provider

import com.bandu.tiji.ai.api.config.ProviderValidation
import com.bandu.tiji.ai.api.config.ResolvedAiConfiguration
import com.bandu.tiji.ai.api.model.AiStreamEvent
import com.bandu.tiji.ai.api.model.AnalyzeImageRequest
import com.bandu.tiji.ai.api.model.AnalyzedQuestion
import com.bandu.tiji.ai.api.model.ExerciseGrade
import com.bandu.tiji.ai.api.model.ExerciseRequest
import com.bandu.tiji.ai.api.model.GeneratedExercise
import com.bandu.tiji.ai.api.model.GradeExerciseRequest
import com.bandu.tiji.ai.api.model.TutorRequest
import com.bandu.tiji.core.model.enums.AiProviderType
import com.bandu.tiji.core.model.enums.MistakeStatus
import com.bandu.tiji.core.model.id.TutorSessionId
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertThrows
import org.junit.Test

class AiProviderRegistryTest {
    @Test
    fun `registry resolves provider by type`() {
        val gemini = FakeProvider(AiProviderType.GEMINI)
        val openAi = FakeProvider(AiProviderType.OPENAI_COMPATIBLE)
        val registry = AiProviderRegistry(listOf(gemini, openAi))

        assertThat(registry.require(AiProviderType.GEMINI)).isSameInstanceAs(gemini)
        assertThat(registry.require(AiProviderType.OPENAI_COMPATIBLE)).isSameInstanceAs(openAi)
        assertThat(registry.registeredTypes()).containsExactly(
            AiProviderType.GEMINI,
            AiProviderType.OPENAI_COMPATIBLE,
        )
    }

    @Test
    fun `duplicate provider types are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            AiProviderRegistry(
                listOf(
                    FakeProvider(AiProviderType.GEMINI),
                    FakeProvider(AiProviderType.GEMINI),
                ),
            )
        }
    }

    private class FakeProvider(
        override val type: AiProviderType,
    ) : AiProvider {
        override suspend fun validate(configuration: ResolvedAiConfiguration): ProviderValidation =
            ProviderValidation.Success

        override suspend fun analyzeImage(
            configuration: ResolvedAiConfiguration,
            request: AnalyzeImageRequest,
        ): AnalyzedQuestion = AnalyzedQuestion(
            subject = "数学",
            knowledgePoints = emptyList(),
            requiresImage = false,
            wrongAnswerText = "",
            mistakeStatus = MistakeStatus.UNKNOWN,
            mistakeAnalysis = "",
            questionText = "q",
            answerText = "a",
            analysis = "n",
        )

        override fun streamTutor(
            configuration: ResolvedAiConfiguration,
            request: TutorRequest,
        ): Flow<AiStreamEvent> = emptyFlow()

        override suspend fun generateExercise(
            configuration: ResolvedAiConfiguration,
            request: ExerciseRequest,
        ): GeneratedExercise = GeneratedExercise("q", "a", "n")

        override suspend fun gradeExercise(
            configuration: ResolvedAiConfiguration,
            request: GradeExerciseRequest,
        ): ExerciseGrade = throw UnsupportedOperationException()
    }
}
