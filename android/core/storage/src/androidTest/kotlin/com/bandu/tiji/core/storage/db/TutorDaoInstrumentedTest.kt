package com.bandu.tiji.core.storage.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bandu.tiji.core.storage.db.entity.CollectionEntity
import com.bandu.tiji.core.storage.db.entity.ErrorItemEntity
import com.bandu.tiji.core.storage.db.entity.ExerciseEntity
import com.bandu.tiji.core.storage.db.entity.TutorMessageEntity
import com.bandu.tiji.core.storage.db.entity.TutorSessionEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TutorDaoInstrumentedTest {
    private lateinit var database: LearningDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LearningDatabase::class.java,
        ).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun deletingSession_cascadesMessagesAndExercises() = runBlocking {
        seedErrorItem()
        val dao = database.tutorDao()
        val session = session()
        dao.insertSession(session)
        dao.insertMessage(message("message-1", 1))
        dao.insertMessage(message("message-2", 2))
        dao.insertExercise(exercise())

        assertThat(dao.getMessages(session.id)).containsExactly(
            message("message-1", 1),
            message("message-2", 2),
        ).inOrder()
        assertThat(dao.getExercises(session.id)).containsExactly(exercise())

        dao.deleteSession(session)

        assertThat(dao.getSession(session.id)).isNull()
        assertThat(dao.messageCount()).isEqualTo(0)
        assertThat(dao.exerciseCount()).isEqualTo(0)
    }

    @Test
    fun deletingErrorItem_setsSessionAndExerciseReferencesToNull() = runBlocking {
        val errorItem = seedErrorItem()
        val dao = database.tutorDao()
        dao.insertSession(session())
        dao.insertMessage(message("message-1", 1))
        dao.insertExercise(exercise())

        database.errorItemDao().delete(errorItem)

        assertThat(dao.getSession("session-1")!!.errorItemId).isNull()
        assertThat(dao.getExercises("session-1").single().sourceErrorItemId).isNull()
        assertThat(dao.getMessages("session-1")).hasSize(1)
    }

    @Test
    fun updatesSessionAndExerciseWithoutLosingOfflineHistory(): Unit = runBlocking {
        seedErrorItem()
        val dao = database.tutorDao()
        val session = session()
        val exercise = exercise()
        dao.insertSession(session)
        dao.insertExercise(exercise)

        val renamed = session.copy(title = "二次函数辅导", updatedAt = 200)
        val graded = exercise.copy(
            userAnswer = "42",
            aiResult = "CORRECT",
            gradingFeedback = "回答正确",
            gradedAt = 300,
        )
        assertThat(dao.updateSession(renamed)).isEqualTo(1)
        assertThat(dao.updateExercise(graded)).isEqualTo(1)

        assertThat(dao.getSession(session.id)).isEqualTo(renamed)
        assertThat(dao.getExercises(session.id)).containsExactly(graded)
    }

    private suspend fun seedErrorItem(): ErrorItemEntity {
        database.collectionDao().insert(CollectionEntity("collection-1", "数学", 1, 1))
        return ErrorItemEntity(
            id = "error-1",
            collectionId = "collection-1",
            imagePath = null,
            imageSha256 = null,
            imageWidth = null,
            imageHeight = null,
            questionText = "题目",
            answerText = "答案",
            analysis = "解析",
            mistakeStatus = "UNKNOWN",
            subject = "数学",
            gradeSemester = null,
            paperLevel = null,
            masteryLevel = 0,
            createdAt = 1,
            updatedAt = 1,
        ).also { database.errorItemDao().insert(it) }
    }

    private fun session() = TutorSessionEntity(
        id = "session-1",
        title = "函数辅导",
        errorItemId = "error-1",
        createdAt = 10,
        updatedAt = 10,
    )

    private fun message(id: String, sequence: Int) = TutorMessageEntity(
        id = id,
        sessionId = "session-1",
        role = if (sequence % 2 == 1) "USER" else "ASSISTANT",
        content = "消息 $sequence",
        status = "COMPLETE",
        sequence = sequence,
        createdAt = sequence.toLong(),
    )

    private fun exercise() = ExerciseEntity(
        id = "exercise-1",
        sessionId = "session-1",
        sourceErrorItemId = "error-1",
        subject = "数学",
        difficulty = "MEDIUM",
        questionText = "练习题",
        expectedAnswer = "42",
        analysis = "练习解析",
        userAnswer = null,
        aiResult = null,
        finalResult = null,
        gradingFeedback = null,
        gradedAt = null,
        overriddenAt = null,
        createdAt = 100,
    )
}
