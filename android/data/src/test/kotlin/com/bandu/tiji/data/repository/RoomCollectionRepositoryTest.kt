package com.bandu.tiji.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.bandu.tiji.core.model.id.CollectionId
import com.bandu.tiji.core.storage.db.LearningDatabase
import com.bandu.tiji.core.storage.db.entity.CollectionEntity
import com.bandu.tiji.core.storage.db.entity.ErrorItemEntity
import com.bandu.tiji.core.testing.id.FixedUuidGenerator
import com.bandu.tiji.core.testing.time.TestClock
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class RoomCollectionRepositoryTest {
    private lateinit var database: LearningDatabase
    private lateinit var clock: TestClock
    private lateinit var repository: RoomCollectionRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LearningDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        clock = TestClock(initialEpochMillis = 1_000L)
        repository = RoomCollectionRepository(
            database = database,
            uuidGenerator = FixedUuidGenerator("collection-1"),
            clock = clock,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `create rename and delete update collection flow`() = runTest {
        repository.observeCollections().test {
            assertThat(awaitItem()).isEmpty()

            val id = repository.create("默认题集")
            assertThat(id).isEqualTo(CollectionId("collection-1"))
            val created = awaitItem().single()
            assertThat(created.name).isEqualTo("默认题集")
            assertThat(created.errorItemCount).isEqualTo(0)
            assertThat(created.updatedAtEpochMillis).isEqualTo(1_000L)

            clock.setEpochMillis(2_000L)
            repository.rename(id, "数学题集")
            val renamed = awaitItem().single()
            assertThat(renamed.name).isEqualTo("数学题集")
            assertThat(renamed.updatedAtEpochMillis).isEqualTo(2_000L)

            repository.delete(id)
            assertThat(awaitItem()).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `delete collection cascades contained error items in one transaction`() = runTest {
        val collection = CollectionEntity(
            id = "collection-1",
            name = "默认题集",
            createdAt = 1_000L,
            updatedAt = 1_000L,
        )
        database.collectionDao().insert(collection)
        database.errorItemDao().insert(errorItemEntity(collectionId = collection.id))
        assertThat(database.errorItemDao().count()).isEqualTo(1)

        repository.delete(CollectionId(collection.id))

        assertThat(database.collectionDao().getById(collection.id)).isNull()
        assertThat(database.errorItemDao().count()).isEqualTo(0)
    }

    @Test
    fun `observe collections includes current error item count`() = runTest {
        database.collectionDao().insert(
            CollectionEntity(
                id = "collection-1",
                name = "默认题集",
                createdAt = 1_000L,
                updatedAt = 1_000L,
            ),
        )
        database.errorItemDao().insert(errorItemEntity(collectionId = "collection-1"))

        repository.observeCollections().test {
            assertThat(awaitItem().single().errorItemCount).isEqualTo(1)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun errorItemEntity(collectionId: String): ErrorItemEntity =
        ErrorItemEntity(
            id = "error-1",
            collectionId = collectionId,
            imagePath = null,
            imageSha256 = null,
            imageWidth = null,
            imageHeight = null,
            questionText = "1 + 1 = ?",
            answerText = "2",
            analysis = "加法",
            wrongAnswerText = "",
            mistakeStatus = "UNKNOWN",
            mistakeAnalysis = "",
            subject = "数学",
            gradeSemester = null,
            paperLevel = null,
            notes = "",
            masteryLevel = 0,
            createdAt = 1_000L,
            updatedAt = 1_000L,
        )
}
