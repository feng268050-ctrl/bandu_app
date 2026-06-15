package com.bandu.tiji.core.storage.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bandu.tiji.core.storage.db.entity.CollectionEntity
import com.bandu.tiji.core.storage.db.entity.ErrorItemEntity
import com.bandu.tiji.core.storage.db.entity.ExerciseEntity
import com.bandu.tiji.core.storage.db.entity.TutorSessionEntity
import com.bandu.tiji.core.storage.db.projection.DimensionCountProjection
import com.bandu.tiji.core.storage.db.projection.ExerciseTotalsProjection
import com.bandu.tiji.core.storage.db.projection.WrongItemTotalsProjection
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatsDaoInstrumentedTest {
    private lateinit var database: LearningDatabase

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LearningDatabase::class.java,
        ).build()
        database.collectionDao().insert(CollectionEntity("collection-1", "综合", 1, 1))
        database.errorItemDao().insert(errorItem("error-math", "数学", mastery = 2, createdAt = 100))
        database.errorItemDao().insert(errorItem("error-physics", "物理", mastery = 0, createdAt = 200))
        database.errorItemDao().insert(errorItem("error-math-2", "数学", mastery = 2, createdAt = 300))
        database.tutorDao().insertSession(
            TutorSessionEntity("session-1", "练习", null, 1, 1),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun wrongItemAggregates_areDerivedFromCurrentRows(): Unit = runBlocking {
        val dao = database.statsDao()

        assertThat(dao.observeWrongItemTotals().first())
            .isEqualTo(WrongItemTotalsProjection(totalCount = 3, masteredCount = 2))
        assertThat(dao.observeWrongItemSubjectCounts().first()).containsExactly(
            DimensionCountProjection("数学", 2),
            DimensionCountProjection("物理", 1),
        ).inOrder()
        assertThat(dao.countWrongItemsCreatedBetween(150, 301)).isEqualTo(2)
    }

    @Test
    fun exerciseAccuracy_excludesNeedsReviewAndUngradedFromDenominator(): Unit = runBlocking {
        val dao = database.tutorDao()
        dao.insertExercise(exercise("correct", aiResult = "CORRECT", createdAt = 1_000))
        dao.insertExercise(exercise("incorrect", aiResult = "INCORRECT", createdAt = 2_000))
        dao.insertExercise(exercise("review", aiResult = "NEEDS_REVIEW", createdAt = 3_000))
        dao.insertExercise(
            exercise(
                "overridden",
                aiResult = "CORRECT",
                finalResult = "INCORRECT",
                createdAt = 4_000,
            ),
        )
        dao.insertExercise(exercise("ungraded", aiResult = null, createdAt = 5_000))

        assertThat(database.statsDao().observeExerciseTotals().first()).isEqualTo(
            ExerciseTotalsProjection(
                totalCount = 5,
                gradedCount = 3,
                correctCount = 1,
            ),
        )
    }

    @Test
    fun exerciseDimensionsRangesAndActiveDays_areAggregatedInSql(): Unit = runBlocking {
        val dayMillis = 86_400_000L
        database.tutorDao().insertExercise(
            exercise(
                id = "math-easy",
                aiResult = "CORRECT",
                createdAt = dayMillis + 1_000,
                subject = "数学",
                difficulty = "EASY",
            ),
        )
        database.tutorDao().insertExercise(
            exercise(
                id = "math-hard",
                aiResult = "INCORRECT",
                createdAt = dayMillis + 2_000,
                subject = "数学",
                difficulty = "HARD",
            ),
        )
        database.tutorDao().insertExercise(
            exercise(
                id = "physics-hard",
                aiResult = "CORRECT",
                createdAt = dayMillis * 2 + 1_000,
                subject = "物理",
                difficulty = "HARD",
            ),
        )
        val stats = database.statsDao()

        assertThat(stats.observeExerciseSubjectCounts().first()).containsExactly(
            DimensionCountProjection("数学", 2),
            DimensionCountProjection("物理", 1),
        ).inOrder()
        assertThat(stats.observeExerciseDifficultyCounts().first()).containsExactly(
            DimensionCountProjection("HARD", 2),
            DimensionCountProjection("EASY", 1),
        ).inOrder()
        assertThat(stats.countExercisesCreatedBetween(dayMillis, dayMillis * 3)).isEqualTo(3)
        assertThat(
            stats.countActiveExerciseDays(
                startInclusive = dayMillis,
                endExclusive = dayMillis * 3,
                utcOffsetModifier = "+00:00",
            ),
        ).isEqualTo(2)
    }

    private fun errorItem(
        id: String,
        subject: String,
        mastery: Int,
        createdAt: Long,
    ) = ErrorItemEntity(
        id = id,
        collectionId = "collection-1",
        imagePath = null,
        imageSha256 = null,
        imageWidth = null,
        imageHeight = null,
        questionText = id,
        answerText = "answer",
        analysis = "analysis",
        mistakeStatus = "UNKNOWN",
        subject = subject,
        gradeSemester = null,
        paperLevel = null,
        masteryLevel = mastery,
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    private fun exercise(
        id: String,
        aiResult: String?,
        finalResult: String? = null,
        createdAt: Long,
        subject: String = "数学",
        difficulty: String = "MEDIUM",
    ) = ExerciseEntity(
        id = id,
        sessionId = "session-1",
        sourceErrorItemId = null,
        subject = subject,
        difficulty = difficulty,
        questionText = id,
        expectedAnswer = "answer",
        analysis = "analysis",
        userAnswer = null,
        aiResult = aiResult,
        finalResult = finalResult,
        gradingFeedback = null,
        gradedAt = if (aiResult == null) null else createdAt,
        overriddenAt = if (finalResult == null) null else createdAt,
        createdAt = createdAt,
    )
}
