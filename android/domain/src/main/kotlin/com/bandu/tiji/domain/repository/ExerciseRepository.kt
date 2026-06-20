package com.bandu.tiji.domain.repository

import com.bandu.tiji.core.model.enums.GradeResult
import com.bandu.tiji.core.model.id.ExerciseId
import com.bandu.tiji.core.model.tutor.Exercise
import com.bandu.tiji.core.model.tutor.ExerciseDraft
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun observe(id: ExerciseId): Flow<Exercise?>

    suspend fun create(draft: ExerciseDraft): ExerciseId

    suspend fun recordGrade(
        id: ExerciseId,
        userAnswer: String,
        result: GradeResult,
        feedback: String,
    )

    suspend fun overrideResult(
        id: ExerciseId,
        result: GradeResult,
    )
}
