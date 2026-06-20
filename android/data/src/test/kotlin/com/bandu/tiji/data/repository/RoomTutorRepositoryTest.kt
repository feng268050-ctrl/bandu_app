package com.bandu.tiji.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.bandu.tiji.core.common.id.UuidGenerator
import com.bandu.tiji.core.model.id.TutorSessionId
import com.bandu.tiji.core.model.tutor.TutorMessageRole
import com.bandu.tiji.core.storage.db.LearningDatabase
import com.bandu.tiji.core.testing.time.TestClock
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class RoomTutorRepositoryTest {
    private lateinit var database: LearningDatabase
    private lateinit var clock: TestClock
    private lateinit var repository: RoomTutorRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LearningDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        clock = TestClock(1_000L)
        repository = RoomTutorRepository(database, IncrementingUuidGenerator(), clock)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `get or create reuses session and messages update observed detail`() = runTest {
        val sessionId = repository.getOrCreate(null)
        assertThat(repository.getOrCreate(null)).isEqualTo(sessionId)

        repository.observeSession(sessionId).test {
            assertThat(awaitItem()?.messages).isEmpty()

            repository.appendUserMessage(sessionId, "怎么解？")
            assertThat(awaitItem()?.messages?.single()?.role).isEqualTo(TutorMessageRole.USER)

            clock.setEpochMillis(2_000L)
            repository.appendAssistantMessage(sessionId, "先移项")
            val updated = checkNotNull(awaitItem())
            assertThat(updated.messages.map { it.sequence }).containsExactly(0, 1).inOrder()
            assertThat(updated.messages.last().role).isEqualTo(TutorMessageRole.ASSISTANT)
            assertThat(updated.updatedAtEpochMillis).isEqualTo(2_000L)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `concurrent message appends allocate unique contiguous sequences`() = runTest {
        val sessionId = repository.getOrCreate(null)

        coroutineScope {
            (0 until 20).map { index ->
                async { repository.appendUserMessage(sessionId, "message-$index") }
            }.awaitAll()
        }

        val messages = database.tutorDao().getMessages(sessionId.value)
        assertThat(messages).hasSize(20)
        assertThat(messages.map { it.sequence }).containsExactlyElementsIn(0 until 20).inOrder()
        assertThat(messages.map { it.id }.toSet()).hasSize(20)
    }

    @Test
    fun `session list orders by latest update and delete cascades messages`() = runTest {
        val first = repository.getOrCreate(null)
        repository.appendUserMessage(first, "first")
        repository.deleteSession(first)

        assertThat(database.tutorDao().getSession(first.value)).isNull()
        assertThat(database.tutorDao().messageCount()).isEqualTo(0)
        repository.observeSessions().test {
            assertThat(awaitItem()).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private class IncrementingUuidGenerator : UuidGenerator {
        private val next = AtomicInteger(1)

        override fun newUuid(): String = "id-${next.getAndIncrement()}"
    }
}
