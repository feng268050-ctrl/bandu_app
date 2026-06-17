package com.bandu.tiji.data.mapper

import com.bandu.tiji.core.model.enums.ExerciseDifficulty
import com.bandu.tiji.core.model.enums.GradeResult
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.model.id.ExerciseId
import com.bandu.tiji.core.model.id.TagId
import com.bandu.tiji.core.model.id.TutorMessageId
import com.bandu.tiji.core.model.id.TutorSessionId
import com.bandu.tiji.core.model.stats.MonthlyCount
import com.bandu.tiji.core.model.tag.TagSummary
import com.bandu.tiji.core.model.tutor.Exercise
import com.bandu.tiji.core.model.tutor.TutorMessage
import com.bandu.tiji.core.model.tutor.TutorMessageRole
import com.bandu.tiji.core.model.tutor.TutorMessageStatus
import com.bandu.tiji.core.storage.db.entity.TagEntity
import com.bandu.tiji.core.storage.db.entity.TutorSessionEntity
import com.bandu.tiji.core.storage.db.projection.DimensionCountProjection
import com.bandu.tiji.core.storage.db.projection.ExerciseTotalsProjection
import com.bandu.tiji.core.storage.db.projection.WrongItemTotalsProjection
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LearningMappersTest {
    @Test
    fun `tag entities map to stable tree order and summaries`() {
        val root = tagEntity(id = "root", name = "数学", parentId = null, sortOrder = 0, isSystem = true)
        val geometry = tagEntity(id = "geometry", name = "几何", parentId = "root", sortOrder = 2)
        val algebra = tagEntity(id = "algebra", name = "代数", parentId = "root", sortOrder = 1)

        val tree = listOf(geometry, root, algebra).toDomainTree(linkedCounts = mapOf("algebra" to 2))

        assertThat(root.toDomainSummary()).isEqualTo(
            TagSummary(TagId("root"), "数学", "数学", isSystem = true),
        )
        assertThat(tree).hasSize(1)
        assertThat(tree.single().children.map { it.tag.id.value }).containsExactly("algebra", "geometry").inOrder()
        assertThat(tree.single().children.first().linkedErrorItemCount).isEqualTo(2)
    }

    @Test
    fun `tag summary maps back to entity`() {
        val summary = TagSummary(TagId("tag-1"), "函数", "数学", isSystem = false)

        val entity = summary.toEntity(
            parentId = TagId("parent"),
            sortOrder = 7,
            code = "MATH-1",
            createdAtEpochMillis = 100L,
            updatedAtEpochMillis = 200L,
        )

        assertThat(entity.id).isEqualTo("tag-1")
        assertThat(entity.parentId).isEqualTo("parent")
        assertThat(entity.sortOrder).isEqualTo(7)
        assertThat(entity.isSystem).isFalse()
        assertThat(entity.updatedAt).isEqualTo(200L)
    }

    @Test
    fun `tutor session and message mappers preserve role and status enums`() {
        val sessionEntity = TutorSessionEntity(
            id = "session-1",
            title = "错题辅导",
            errorItemId = "error-1",
            createdAt = 100L,
            updatedAt = 200L,
        )
        val message = TutorMessage(
            id = TutorMessageId("message-1"),
            role = TutorMessageRole.ASSISTANT,
            content = "先看条件",
            status = TutorMessageStatus.COMPLETE,
            sequence = 0,
            createdAtEpochMillis = 120L,
        )

        val mappedMessage = message.toEntity(TutorSessionId("session-1")).toDomain()
        val session = sessionEntity.toDomain(messages = listOf(mappedMessage), exercises = emptyList())

        assertThat(sessionEntity.toDomainSummary().errorItemId).isEqualTo(ErrorItemId("error-1"))
        assertThat(mappedMessage).isEqualTo(message)
        assertThat(session.messages).containsExactly(message)
        assertThat(session.updatedAtEpochMillis).isEqualTo(200L)
    }

    @Test
    fun `exercise mapper round trips all grade result enum values`() {
        GradeResult.entries.forEachIndexed { index, gradeResult ->
            val exercise = exercise(
                id = "exercise-$index",
                difficulty = ExerciseDifficulty.MEDIUM,
                aiResult = gradeResult,
                finalResult = gradeResult,
            )

            assertThat(exercise.toEntity().toDomain()).isEqualTo(exercise)
        }
    }

    @Test
    fun `exercise mapper accepts all difficulty enum values from storage`() {
        ExerciseDifficulty.entries.forEachIndexed { index, difficulty ->
            val entity = exercise(id = "exercise-$index", difficulty = difficulty).toEntity()

            assertThat(entity.toDomain().difficulty).isEqualTo(difficulty)
        }
    }

    @Test
    fun `stats projections map totals dimensions and monthly counts`() {
        val wrongStats = WrongItemTotalsProjection(totalCount = 8, masteredCount = 3)
            .toDomain(
                subjectCounts = listOf(DimensionCountProjection("数学", 5)),
                monthlyNewCounts = listOf(MonthlyCount(2026, 6, 4)),
            )
        val exerciseStats = ExerciseTotalsProjection(totalCount = 6, gradedCount = 5, correctCount = 4)
            .toDomain(
                subjectCounts = listOf(DimensionCountProjection("数学", 6)),
                difficultyCounts = listOf(DimensionCountProjection(ExerciseDifficulty.HARD.name, 2)),
                monthlyPracticeCounts = listOf(MonthlyCount(2026, 6, 2)),
                activeDaysLastSixMonths = 3,
            )

        assertThat(wrongStats.masteryRate).isEqualTo(3.0 / 8.0)
        assertThat(wrongStats.subjectCounts).containsExactly("数学", 5)
        assertThat(wrongStats.monthlyNewCounts.single().count).isEqualTo(4)
        assertThat(exerciseStats.accuracyRate).isEqualTo(4.0 / 5.0)
        assertThat(exerciseStats.difficultyCounts).containsExactly(ExerciseDifficulty.HARD, 2)
        assertThat(exerciseStats.activeDaysLastSixMonths).isEqualTo(3)
    }

    private fun tagEntity(
        id: String,
        name: String,
        parentId: String?,
        sortOrder: Int,
        isSystem: Boolean = false,
    ): TagEntity =
        TagEntity(
            id = id,
            name = name,
            subject = "数学",
            parentId = parentId,
            sortOrder = sortOrder,
            code = null,
            isSystem = isSystem,
            createdAt = 100L,
            updatedAt = 100L,
        )

    private fun exercise(
        id: String,
        difficulty: ExerciseDifficulty,
        aiResult: GradeResult? = GradeResult.CORRECT,
        finalResult: GradeResult? = null,
    ): Exercise =
        Exercise(
            id = ExerciseId(id),
            sessionId = TutorSessionId("session-1"),
            sourceErrorItemId = ErrorItemId("error-1"),
            subject = "数学",
            difficulty = difficulty,
            questionText = "1 + 1 = ?",
            expectedAnswer = "2",
            analysis = "加法",
            userAnswer = "2",
            aiResult = aiResult,
            finalResult = finalResult,
            gradingFeedback = "正确",
            gradedAtEpochMillis = 200L,
            overriddenAtEpochMillis = if (finalResult == null) null else 300L,
            createdAtEpochMillis = 100L,
        )
}
