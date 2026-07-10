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
import com.bandu.tiji.ai.api.model.SplitQuestionBankPage
import com.bandu.tiji.ai.api.model.SplitQuestionBankPageRequest
import com.bandu.tiji.ai.api.model.TutorRequest
import com.bandu.tiji.core.model.enums.AiProviderType
import kotlinx.coroutines.flow.Flow

interface AiProvider {
    val type: AiProviderType

    suspend fun validate(configuration: ResolvedAiConfiguration): ProviderValidation

    suspend fun analyzeImage(
        configuration: ResolvedAiConfiguration,
        request: AnalyzeImageRequest,
    ): AnalyzedQuestion

    fun streamTutor(
        configuration: ResolvedAiConfiguration,
        request: TutorRequest,
    ): Flow<AiStreamEvent>

    suspend fun generateExercise(
        configuration: ResolvedAiConfiguration,
        request: ExerciseRequest,
    ): GeneratedExercise

    suspend fun gradeExercise(
        configuration: ResolvedAiConfiguration,
        request: GradeExerciseRequest,
    ): ExerciseGrade

    suspend fun splitQuestionBankPage(
        configuration: ResolvedAiConfiguration,
        request: SplitQuestionBankPageRequest,
    ): SplitQuestionBankPage
}
