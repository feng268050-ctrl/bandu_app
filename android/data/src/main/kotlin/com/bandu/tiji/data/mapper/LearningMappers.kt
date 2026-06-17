package com.bandu.tiji.data.mapper

import com.bandu.tiji.core.model.enums.ExerciseDifficulty
import com.bandu.tiji.core.model.enums.GradeResult
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.model.id.ExerciseId
import com.bandu.tiji.core.model.id.TagId
import com.bandu.tiji.core.model.id.TutorMessageId
import com.bandu.tiji.core.model.id.TutorSessionId
import com.bandu.tiji.core.model.stats.ExerciseStats
import com.bandu.tiji.core.model.stats.MonthlyCount
import com.bandu.tiji.core.model.stats.WrongItemStats
import com.bandu.tiji.core.model.tag.TagNode
import com.bandu.tiji.core.model.tag.TagSummary
import com.bandu.tiji.core.model.tutor.Exercise
import com.bandu.tiji.core.model.tutor.TutorMessage
import com.bandu.tiji.core.model.tutor.TutorMessageRole
import com.bandu.tiji.core.model.tutor.TutorMessageStatus
import com.bandu.tiji.core.model.tutor.TutorSession
import com.bandu.tiji.core.model.tutor.TutorSessionSummary
import com.bandu.tiji.core.storage.db.entity.ExerciseEntity
import com.bandu.tiji.core.storage.db.entity.TagEntity
import com.bandu.tiji.core.storage.db.entity.TutorMessageEntity
import com.bandu.tiji.core.storage.db.entity.TutorSessionEntity
import com.bandu.tiji.core.storage.db.projection.DimensionCountProjection
import com.bandu.tiji.core.storage.db.projection.ExerciseTotalsProjection
import com.bandu.tiji.core.storage.db.projection.WrongItemTotalsProjection

internal fun TagEntity.toDomainSummary(): TagSummary =
    TagSummary(
        id = TagId(id),
        name = name,
        subject = subject,
        isSystem = isSystem,
    )

internal fun TagSummary.toEntity(
    parentId: TagId?,
    sortOrder: Int,
    code: String?,
    createdAtEpochMillis: Long,
    updatedAtEpochMillis: Long = createdAtEpochMillis,
): TagEntity =
    TagEntity(
        id = id.value,
        name = name,
        subject = subject,
        parentId = parentId?.value,
        sortOrder = sortOrder,
        code = code,
        isSystem = isSystem,
        createdAt = createdAtEpochMillis,
        updatedAt = updatedAtEpochMillis,
    )

internal fun List<TagEntity>.toDomainTree(
    linkedCounts: Map<String, Int> = emptyMap(),
): List<TagNode> {
    val childrenByParent = sortedWith(compareBy<TagEntity> { it.sortOrder }.thenBy { it.name.lowercase() })
        .groupBy { it.parentId }

    fun buildNode(entity: TagEntity): TagNode =
        TagNode(
            tag = entity.toDomainSummary(),
            code = entity.code,
            sortOrder = entity.sortOrder,
            linkedErrorItemCount = linkedCounts[entity.id] ?: 0,
            children = childrenByParent[entity.id].orEmpty().map(::buildNode),
        )

    return childrenByParent[null].orEmpty().map(::buildNode)
}

internal fun TutorSessionEntity.toDomainSummary(): TutorSessionSummary =
    TutorSessionSummary(
        id = TutorSessionId(id),
        title = title,
        errorItemId = errorItemId?.let(::ErrorItemId),
        updatedAtEpochMillis = updatedAt,
    )

internal fun TutorSessionEntity.toDomain(
    messages: List<TutorMessage>,
    exercises: List<Exercise>,
): TutorSession =
    TutorSession(
        id = TutorSessionId(id),
        title = title,
        errorItemId = errorItemId?.let(::ErrorItemId),
        messages = messages,
        exercises = exercises,
        createdAtEpochMillis = createdAt,
        updatedAtEpochMillis = updatedAt,
    )

internal fun TutorMessage.toEntity(sessionId: TutorSessionId): TutorMessageEntity =
    TutorMessageEntity(
        id = id.value,
        sessionId = sessionId.value,
        role = role.name,
        content = content,
        status = status.name,
        sequence = sequence,
        createdAt = createdAtEpochMillis,
    )

internal fun TutorMessageEntity.toDomain(): TutorMessage =
    TutorMessage(
        id = TutorMessageId(id),
        role = TutorMessageRole.valueOf(role),
        content = content,
        status = TutorMessageStatus.valueOf(status),
        sequence = sequence,
        createdAtEpochMillis = createdAt,
    )

internal fun Exercise.toEntity(): ExerciseEntity =
    ExerciseEntity(
        id = id.value,
        sessionId = sessionId.value,
        sourceErrorItemId = sourceErrorItemId?.value,
        subject = subject,
        difficulty = difficulty.name,
        questionText = questionText,
        expectedAnswer = expectedAnswer,
        analysis = analysis,
        userAnswer = userAnswer,
        aiResult = aiResult?.name,
        finalResult = finalResult?.name,
        gradingFeedback = gradingFeedback,
        gradedAt = gradedAtEpochMillis,
        overriddenAt = overriddenAtEpochMillis,
        createdAt = createdAtEpochMillis,
    )

internal fun ExerciseEntity.toDomain(): Exercise =
    Exercise(
        id = ExerciseId(id),
        sessionId = TutorSessionId(sessionId),
        sourceErrorItemId = sourceErrorItemId?.let(::ErrorItemId),
        subject = subject,
        difficulty = ExerciseDifficulty.valueOf(difficulty),
        questionText = questionText,
        expectedAnswer = expectedAnswer,
        analysis = analysis,
        userAnswer = userAnswer,
        aiResult = aiResult?.let(GradeResult::valueOf),
        finalResult = finalResult?.let(GradeResult::valueOf),
        gradingFeedback = gradingFeedback,
        gradedAtEpochMillis = gradedAt,
        overriddenAtEpochMillis = overriddenAt,
        createdAtEpochMillis = createdAt,
    )

internal fun WrongItemTotalsProjection.toDomain(
    subjectCounts: List<DimensionCountProjection>,
    monthlyNewCounts: List<MonthlyCount>,
): WrongItemStats =
    WrongItemStats(
        totalCount = totalCount,
        masteredCount = masteredCount,
        subjectCounts = subjectCounts.toCountMap(),
        monthlyNewCounts = monthlyNewCounts,
    )

internal fun ExerciseTotalsProjection.toDomain(
    subjectCounts: List<DimensionCountProjection>,
    difficultyCounts: List<DimensionCountProjection>,
    monthlyPracticeCounts: List<MonthlyCount>,
    activeDaysLastSixMonths: Int,
): ExerciseStats =
    ExerciseStats(
        totalCount = totalCount,
        gradedCount = gradedCount,
        correctCount = correctCount,
        subjectCounts = subjectCounts.toCountMap(),
        difficultyCounts = difficultyCounts.toDifficultyCountMap(),
        monthlyPracticeCounts = monthlyPracticeCounts,
        activeDaysLastSixMonths = activeDaysLastSixMonths,
    )

internal fun List<DimensionCountProjection>.toCountMap(): Map<String, Int> =
    associate { projection -> projection.dimension to projection.count }

internal fun List<DimensionCountProjection>.toDifficultyCountMap(): Map<ExerciseDifficulty, Int> =
    associate { projection -> ExerciseDifficulty.valueOf(projection.dimension) to projection.count }
