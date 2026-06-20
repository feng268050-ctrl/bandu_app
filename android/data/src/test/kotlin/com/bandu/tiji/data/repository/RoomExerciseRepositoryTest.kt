package com.bandu.tiji.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.bandu.tiji.core.model.enums.ExerciseDifficulty
import com.bandu.tiji.core.model.enums.GradeResult
import com.bandu.tiji.core.model.id.TutorSessionId
import com.bandu.tiji.core.model.tutor.ExerciseDraft
import com.bandu.tiji.core.storage.db.LearningDatabase
import com.bandu.tiji.core.storage.db.entity.TutorSessionEntity
import com.bandu.tiji.core.testing.id.FixedUuidGenerator
import com.bandu.tiji.core.testing.time.TestClock
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class RoomExerciseRepositoryTest {
    private lateinit var database: LearningDatabase
    private lateinit var clock: TestClock
    private lateinit var repository: RoomExerciseRepository

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LearningDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        database.tutorDao().insertSession(
            TutorSessionEntity("session-1", "练习", null, 100L, 100L),
        )
        clock = TestClock(1_000L)
        repository = RoomExerciseRepository(
            database,
            FixedUuidGenerator("exercise-1"),
            clock,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `create grade and override expose effective final result`() = runTest {
        val id = repository.create(draft())

        repository.observe(id).test {
            val created = checkNotNull(awaitItem())
            assertThat(created.aiResult).isNull()
            assertThat(created.effectiveResult).isNull()

            clock.setEpochMillis(2_000L)
            repository.recordGrade(id, "3", GradeResult.INCORRECT, "请重新计算")
            val graded = checkNotNull(awaitItem())
            assertThat(graded.userAnswer).isEqualTo("3")
            assertThat(graded.aiResult).isEqualTo(GradeResult.INCORRECT)
            assertThat(graded.effectiveResult).isEqualTo(GradeResult.INCORRECT)
            assertThat(graded.gradedAtEpochMillis).isEqualTo(2_000L)

            clock.setEpochMillis(3_000L)
            repository.overrideResult(id, GradeResult.CORRECT)
            val overridden = checkNotNull(awaitItem())
            assertThat(overridden.finalResult).isEqualTo(GradeResult.CORRECT)
            assertThat(overridden.effectiveResult).isEqualTo(GradeResult.CORRECT)
            assertThat(overridden.overriddenAtEpochMillis).isEqualTo(3_000L)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `manual override changes statistics effective result`() = runTest {
        val id = repository.create(draft())
        repository.recordGrade(id, "3", GradeResult.INCORRECT, "错误")
        val before = database.statsDao().observeExerciseTotals().first()
        assertThat(before.gradedCount).isEqualTo(1)
        assertThat(before.correctCount).isEqualTo(0)

        repository.overrideResult(id, GradeResult.CORRECT)

        val after = database.statsDao().observeExerciseTotals().first()
        assertThat(after.gradedCount).isEqualTo(1)
        assertThat(after.correctCount).isEqualTo(1)
    }

    private fun draft(): ExerciseDraft =
        ExerciseDraft(
            sessionId = TutorSessionId("session-1"),
            sourceErrorItemId = null,
            subject = "数学",
            difficulty = ExerciseDifficulty.MEDIUM,
            questionText = "1 + 1 = ?",
            expectedAnswer = "2",
            analysis = "加法",
        )
}
