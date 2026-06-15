package com.bandu.tiji.core.testing.fixture

import com.bandu.tiji.core.model.collection.CollectionSummary
import com.bandu.tiji.core.model.enums.ExerciseDifficulty
import com.bandu.tiji.core.model.enums.GradeResult
import com.bandu.tiji.core.model.enums.MasteryLevel
import com.bandu.tiji.core.model.enums.MistakeStatus
import com.bandu.tiji.core.model.enums.PaperLevel
import com.bandu.tiji.core.model.erroritem.ErrorItem
import com.bandu.tiji.core.model.erroritem.StoredImage
import com.bandu.tiji.core.model.id.CollectionId
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.model.id.ExerciseId
import com.bandu.tiji.core.model.id.TagId
import com.bandu.tiji.core.model.id.TutorSessionId
import com.bandu.tiji.core.model.tag.TagSummary
import com.bandu.tiji.core.model.tutor.Exercise
import com.bandu.tiji.core.model.tutor.TutorMessage
import com.bandu.tiji.core.model.tutor.TutorSession

const val DEFAULT_FIXTURE_EPOCH_MILLIS: Long = 1_700_000_000_000L

fun collectionFixture(
    block: CollectionFixtureBuilder.() -> Unit = {},
): CollectionSummary = CollectionFixtureBuilder().apply(block).build()

@BanduTestFixtureDsl
class CollectionFixtureBuilder {
    var id: CollectionId = CollectionId("collection-1")
    var name: String = "Collection"
    var errorItemCount: Int = 0
    var updatedAtEpochMillis: Long = DEFAULT_FIXTURE_EPOCH_MILLIS

    fun build(): CollectionSummary =
        CollectionSummary(
            id = id,
            name = name,
            errorItemCount = errorItemCount,
            updatedAtEpochMillis = updatedAtEpochMillis,
        )
}

fun tagFixture(
    block: TagFixtureBuilder.() -> Unit = {},
): TagSummary = TagFixtureBuilder().apply(block).build()

@BanduTestFixtureDsl
class TagFixtureBuilder {
    var id: TagId = TagId("tag-1")
    var name: String = "Algebra"
    var subject: String = "Math"
    var isSystem: Boolean = false

    fun build(): TagSummary =
        TagSummary(
            id = id,
            name = name,
            subject = subject,
            isSystem = isSystem,
        )
}

fun errorItemFixture(
    block: ErrorItemFixtureBuilder.() -> Unit = {},
): ErrorItem = ErrorItemFixtureBuilder().apply(block).build()

@BanduTestFixtureDsl
class ErrorItemFixtureBuilder {
    var id: ErrorItemId = ErrorItemId("error-item-1")
    var collectionId: CollectionId = CollectionId("collection-1")
    var image: StoredImage? = null
    var questionText: String = "What is 1 + 1?"
    var answerText: String = "2"
    var analysis: String = "Add the two values."
    var wrongAnswerText: String = ""
    var mistakeStatus: MistakeStatus = MistakeStatus.UNKNOWN
    var mistakeAnalysis: String = ""
    var subject: String = "Math"
    var tags: List<TagSummary> = listOf(tagFixture())
    var gradeSemester: String? = null
    var paperLevel: PaperLevel? = null
    var notes: String = ""
    var masteryLevel: MasteryLevel = MasteryLevel.NEW
    var createdAtEpochMillis: Long = DEFAULT_FIXTURE_EPOCH_MILLIS
    var updatedAtEpochMillis: Long = DEFAULT_FIXTURE_EPOCH_MILLIS

    fun build(): ErrorItem =
        ErrorItem(
            id = id,
            collectionId = collectionId,
            image = image,
            questionText = questionText,
            answerText = answerText,
            analysis = analysis,
            wrongAnswerText = wrongAnswerText,
            mistakeStatus = mistakeStatus,
            mistakeAnalysis = mistakeAnalysis,
            subject = subject,
            tags = tags,
            gradeSemester = gradeSemester,
            paperLevel = paperLevel,
            notes = notes,
            masteryLevel = masteryLevel,
            createdAtEpochMillis = createdAtEpochMillis,
            updatedAtEpochMillis = updatedAtEpochMillis,
        )
}

fun sessionFixture(
    block: SessionFixtureBuilder.() -> Unit = {},
): TutorSession = SessionFixtureBuilder().apply(block).build()

@BanduTestFixtureDsl
class SessionFixtureBuilder {
    var id: TutorSessionId = TutorSessionId("session-1")
    var title: String = "Tutor session"
    var errorItemId: ErrorItemId? = null
    var messages: List<TutorMessage> = emptyList()
    var exercises: List<Exercise> = emptyList()
    var createdAtEpochMillis: Long = DEFAULT_FIXTURE_EPOCH_MILLIS
    var updatedAtEpochMillis: Long = DEFAULT_FIXTURE_EPOCH_MILLIS

    fun build(): TutorSession =
        TutorSession(
            id = id,
            title = title,
            errorItemId = errorItemId,
            messages = messages,
            exercises = exercises,
            createdAtEpochMillis = createdAtEpochMillis,
            updatedAtEpochMillis = updatedAtEpochMillis,
        )
}

fun exerciseFixture(
    block: ExerciseFixtureBuilder.() -> Unit = {},
): Exercise = ExerciseFixtureBuilder().apply(block).build()

@BanduTestFixtureDsl
class ExerciseFixtureBuilder {
    var id: ExerciseId = ExerciseId("exercise-1")
    var sessionId: TutorSessionId = TutorSessionId("session-1")
    var sourceErrorItemId: ErrorItemId? = null
    var subject: String = "Math"
    var difficulty: ExerciseDifficulty = ExerciseDifficulty.MEDIUM
    var questionText: String = "What is 2 + 2?"
    var expectedAnswer: String = "4"
    var analysis: String = "Add the two values."
    var userAnswer: String? = null
    var aiResult: GradeResult? = null
    var finalResult: GradeResult? = null
    var gradingFeedback: String? = null
    var gradedAtEpochMillis: Long? = null
    var overriddenAtEpochMillis: Long? = null
    var createdAtEpochMillis: Long = DEFAULT_FIXTURE_EPOCH_MILLIS

    fun build(): Exercise =
        Exercise(
            id = id,
            sessionId = sessionId,
            sourceErrorItemId = sourceErrorItemId,
            subject = subject,
            difficulty = difficulty,
            questionText = questionText,
            expectedAnswer = expectedAnswer,
            analysis = analysis,
            userAnswer = userAnswer,
            aiResult = aiResult,
            finalResult = finalResult,
            gradingFeedback = gradingFeedback,
            gradedAtEpochMillis = gradedAtEpochMillis,
            overriddenAtEpochMillis = overriddenAtEpochMillis,
            createdAtEpochMillis = createdAtEpochMillis,
        )
}
