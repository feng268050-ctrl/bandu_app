package com.bandu.tiji.data.repository

import androidx.room.withTransaction
import com.bandu.tiji.core.common.id.UuidGenerator
import com.bandu.tiji.core.common.time.Clock
import com.bandu.tiji.core.model.enums.GradeResult
import com.bandu.tiji.core.model.id.ExerciseId
import com.bandu.tiji.core.model.tutor.Exercise
import com.bandu.tiji.core.model.tutor.ExerciseDraft
import com.bandu.tiji.core.storage.db.LearningDatabase
import com.bandu.tiji.core.storage.db.entity.ExerciseEntity
import com.bandu.tiji.data.mapper.toDomain
import com.bandu.tiji.domain.repository.ExerciseRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class RoomExerciseRepository @Inject constructor(
    private val database: LearningDatabase,
    private val uuidGenerator: UuidGenerator,
    private val clock: Clock,
) : ExerciseRepository {
    private val tutorDao = database.tutorDao()

    override fun observe(id: ExerciseId): Flow<Exercise?> =
        database.invalidationTracker
            .createFlow("exercises", emitInitialState = true)
            .map { database.findExercise(id.value)?.toDomain() }

    override suspend fun create(draft: ExerciseDraft): ExerciseId {
        val id = ExerciseId(uuidGenerator.newUuid())
        tutorDao.insertExercise(
            ExerciseEntity(
                id = id.value,
                sessionId = draft.sessionId.value,
                sourceErrorItemId = draft.sourceErrorItemId?.value,
                subject = draft.subject,
                difficulty = draft.difficulty.name,
                questionText = draft.questionText,
                expectedAnswer = draft.expectedAnswer,
                analysis = draft.analysis,
                userAnswer = null,
                aiResult = null,
                finalResult = null,
                gradingFeedback = null,
                gradedAt = null,
                overriddenAt = null,
                createdAt = clock.nowEpochMillis(),
            ),
        )
        return id
    }

    override suspend fun recordGrade(
        id: ExerciseId,
        userAnswer: String,
        result: GradeResult,
        feedback: String,
    ) {
        database.withTransaction {
            val current = requireNotNull(database.findExercise(id.value)) { "Exercise does not exist" }
            tutorDao.updateExercise(
                current.copy(
                    userAnswer = userAnswer,
                    aiResult = result.name,
                    gradingFeedback = feedback,
                    gradedAt = clock.nowEpochMillis(),
                ),
            )
        }
    }

    override suspend fun overrideResult(
        id: ExerciseId,
        result: GradeResult,
    ) {
        database.withTransaction {
            val current = requireNotNull(database.findExercise(id.value)) { "Exercise does not exist" }
            tutorDao.updateExercise(
                current.copy(
                    finalResult = result.name,
                    overriddenAt = clock.nowEpochMillis(),
                ),
            )
        }
    }

    private fun LearningDatabase.findExercise(id: String): ExerciseEntity? =
        openHelper.readableDatabase
            .query("SELECT * FROM exercises WHERE id = ?", arrayOf(id))
            .use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                ExerciseEntity(
                    id = cursor.stringValue("id"),
                    sessionId = cursor.stringValue("session_id"),
                    sourceErrorItemId = cursor.nullableStringValue("source_error_item_id"),
                    subject = cursor.stringValue("subject"),
                    difficulty = cursor.stringValue("difficulty"),
                    questionText = cursor.stringValue("question_text"),
                    expectedAnswer = cursor.stringValue("expected_answer"),
                    analysis = cursor.stringValue("analysis"),
                    userAnswer = cursor.nullableStringValue("user_answer"),
                    aiResult = cursor.nullableStringValue("ai_result"),
                    finalResult = cursor.nullableStringValue("final_result"),
                    gradingFeedback = cursor.nullableStringValue("grading_feedback"),
                    gradedAt = cursor.nullableLongValue("graded_at"),
                    overriddenAt = cursor.nullableLongValue("overridden_at"),
                    createdAt = cursor.longValue("created_at"),
                )
            }
}

private fun android.database.Cursor.stringValue(column: String): String =
    getString(getColumnIndexOrThrow(column))

private fun android.database.Cursor.nullableStringValue(column: String): String? {
    val index = getColumnIndexOrThrow(column)
    return if (isNull(index)) null else getString(index)
}

private fun android.database.Cursor.longValue(column: String): Long =
    getLong(getColumnIndexOrThrow(column))

private fun android.database.Cursor.nullableLongValue(column: String): Long? {
    val index = getColumnIndexOrThrow(column)
    return if (isNull(index)) null else getLong(index)
}
