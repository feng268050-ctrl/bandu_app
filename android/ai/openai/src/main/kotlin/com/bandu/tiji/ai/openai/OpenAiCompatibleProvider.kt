package com.bandu.tiji.ai.openai

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
import com.bandu.tiji.ai.api.provider.AiProvider
import com.bandu.tiji.core.model.enums.AiProviderType
import kotlinx.coroutines.flow.Flow

class OpenAiCompatibleProvider : AiProvider {
    override val type: AiProviderType = AiProviderType.OPENAI_COMPATIBLE

    override suspend fun validate(
        configuration: ResolvedAiConfiguration,
    ): ProviderValidation = unsupported()

    override suspend fun analyzeImage(
        configuration: ResolvedAiConfiguration,
        request: AnalyzeImageRequest,
    ): AnalyzedQuestion = unsupported()

    override fun streamTutor(
        configuration: ResolvedAiConfiguration,
        request: TutorRequest,
    ): Flow<AiStreamEvent> = unsupported()

    override suspend fun generateExercise(
        configuration: ResolvedAiConfiguration,
        request: ExerciseRequest,
    ): GeneratedExercise = unsupported()

    override suspend fun gradeExercise(
        configuration: ResolvedAiConfiguration,
        request: GradeExerciseRequest,
    ): ExerciseGrade = unsupported()

    private fun <T> unsupported(): T =
        throw UnsupportedOperationException("OpenAI-compatible operation is not implemented")
}
