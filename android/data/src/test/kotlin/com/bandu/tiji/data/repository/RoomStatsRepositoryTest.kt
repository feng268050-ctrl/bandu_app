package com.bandu.tiji.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.bandu.tiji.core.storage.db.LearningDatabase
import com.bandu.tiji.core.storage.db.entity.CollectionEntity
import com.bandu.tiji.core.storage.db.entity.ErrorItemEntity
import com.bandu.tiji.core.storage.db.entity.ExerciseEntity
import com.bandu.tiji.core.storage.db.entity.TutorSessionEntity
import com.bandu.tiji.core.testing.time.TestClock
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class RoomStatsRepositoryTest {
    private lateinit var database: LearningDatabase
    private lateinit var repository: RoomStatsRepository
    private val now = Instant.parse("2026-06-20T08:00:00Z").toEpochMilli()

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LearningDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        database.collectionDao().insert(CollectionEntity("collection-1", "默认题集", now, now))
        database.tutorDao().insertSession(TutorSessionEntity("session-1", "辅导", null, now, now))
        repository = RoomStatsRepository.forTest(
            database = database,
            clock = TestClock(now),
            zoneId = ZoneId.of("UTC"),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `wrong item stats flow automatically updates totals subjects and month`() = runTest {
        repository.observeWrongItemStats().test {
            val initial = awaitItem()
            assertThat(initial.totalCount).isEqualTo(0)
            assertThat(initial.monthlyNewCounts).hasSize(6)

            database.errorItemDao().insert(errorItem())

            var updated = awaitItem()
            while (
                updated.totalCount != 1 ||
                updated.subjectCounts["数学"] != 1 ||
                updated.monthlyNewCounts.last().count != 1
            ) {
                updated = awaitItem()
            }
            assertThat(updated.totalCount).isEqualTo(1)
            assertThat(updated.masteredCount).isEqualTo(1)
            assertThat(updated.subjectCounts).containsExactly("数学", 1)
            assertThat(updated.monthlyNewCounts.last().count).isEqualTo(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `exercise stats flow uses effective grades and local active days`() = runTest {
        repository.observeExerciseStats().test {
            assertThat(awaitItem().totalCount).isEqualTo(0)

            database.tutorDao().insertExercise(exercise())

            var updated = awaitItem()
            while (
                updated.totalCount != 1 ||
                updated.subjectCounts["数学"] != 1 ||
                updated.monthlyPracticeCounts.last().count != 1 ||
                updated.activeDaysLastSixMonths != 1
            ) {
                updated = awaitItem()
            }
            assertThat(updated.totalCount).isEqualTo(1)
            assertThat(updated.gradedCount).isEqualTo(1)
            assertThat(updated.correctCount).isEqualTo(1)
            assertThat(updated.subjectCounts).containsExactly("数学", 1)
            assertThat(updated.monthlyPracticeCounts.last().count).isEqualTo(1)
            assertThat(updated.activeDaysLastSixMonths).isEqualTo(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun errorItem(): ErrorItemEntity =
        ErrorItemEntity(
            id = "error-1",
            collectionId = "collection-1",
            imagePath = null,
            imageSha256 = null,
            imageWidth = null,
            imageHeight = null,
            questionText = "题目",
            answerText = "答案",
            analysis = "解析",
            wrongAnswerText = "",
            mistakeStatus = "UNKNOWN",
            mistakeAnalysis = "",
            subject = "数学",
            gradeSemester = null,
            paperLevel = null,
            notes = "",
            masteryLevel = 2,
            createdAt = now,
            updatedAt = now,
        )

    private fun exercise(): ExerciseEntity =
        ExerciseEntity(
            id = "exercise-1",
            sessionId = "session-1",
            sourceErrorItemId = null,
            subject = "数学",
            difficulty = "MEDIUM",
            questionText = "1 + 1 = ?",
            expectedAnswer = "2",
            analysis = "加法",
            userAnswer = "2",
            aiResult = "INCORRECT",
            finalResult = "CORRECT",
            gradingFeedback = "人工订正",
            gradedAt = now,
            overriddenAt = now,
            createdAt = now,
        )
}
