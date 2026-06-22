package com.bandu.tiji.core.testing.fake

import com.bandu.tiji.core.model.enums.GradeResult
import com.bandu.tiji.core.model.id.ExerciseId
import com.bandu.tiji.core.model.tutor.Exercise
import com.bandu.tiji.core.model.tutor.ExerciseDraft
import com.bandu.tiji.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeExerciseRepository(
    initialExercises: List<Exercise> = emptyList(),
) : ExerciseRepository {
    private val exercises = MutableStateFlow(initialExercises.associateBy { it.id })
    private var nextId = initialExercises.size + 1
    val createdDrafts = mutableListOf<ExerciseDraft>()

    override fun observe(id: ExerciseId): Flow<Exercise?> =
        exercises.map { it[id] }

    override suspend fun create(draft: ExerciseDraft): ExerciseId {
        createdDrafts += draft
        val id = ExerciseId("exercise-${nextId++}")
        exercises.value = exercises.value + (
            id to Exercise(
                id = id,
                sessionId = draft.sessionId,
                sourceErrorItemId = draft.sourceErrorItemId,
                subject = draft.subject,
                difficulty = draft.difficulty,
                questionText = draft.questionText,
                expectedAnswer = draft.expectedAnswer,
                analysis = draft.analysis,
                userAnswer = null,
                aiResult = null,
                finalResult = null,
                gradingFeedback = null,
                gradedAtEpochMillis = null,
                overriddenAtEpochMillis = null,
                createdAtEpochMillis = 0L,
            )
        )
        return id
    }

    override suspend fun recordGrade(
        id: ExerciseId,
        userAnswer: String,
        result: GradeResult,
        feedback: String,
    ) {
        exercises.value = exercises.value.mapValues { (exerciseId, exercise) ->
            if (exerciseId == id) {
                exercise.copy(
                    userAnswer = userAnswer,
                    aiResult = result,
                    gradingFeedback = feedback,
                    gradedAtEpochMillis = 1L,
                )
            } else {
                exercise
            }
        }
    }

    override suspend fun overrideResult(
        id: ExerciseId,
        result: GradeResult,
    ) {
        exercises.value = exercises.value.mapValues { (exerciseId, exercise) ->
            if (exerciseId == id) {
                exercise.copy(finalResult = result, overriddenAtEpochMillis = 2L)
            } else {
                exercise
            }
        }
    }
}
