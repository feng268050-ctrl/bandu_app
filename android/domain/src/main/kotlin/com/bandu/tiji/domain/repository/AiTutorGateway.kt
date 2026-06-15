package com.bandu.tiji.domain.repository

import com.bandu.tiji.domain.ai.AiStreamEvent
import com.bandu.tiji.domain.ai.AnalyzeImageRequest
import com.bandu.tiji.domain.ai.AnalyzedQuestion
import com.bandu.tiji.domain.ai.ExerciseGrade
import com.bandu.tiji.domain.ai.ExerciseRequest
import com.bandu.tiji.domain.ai.GeneratedExercise
import com.bandu.tiji.domain.ai.GradeExerciseRequest
import com.bandu.tiji.domain.ai.TutorRequest
import kotlinx.coroutines.flow.Flow

interface AiTutorGateway {
    suspend fun analyzeImage(request: AnalyzeImageRequest): AnalyzedQuestion

    fun streamTutor(request: TutorRequest): Flow<AiStreamEvent>

    suspend fun generateExercise(request: ExerciseRequest): GeneratedExercise

    suspend fun gradeExercise(request: GradeExerciseRequest): ExerciseGrade
}
