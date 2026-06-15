package com.bandu.tiji.core.storage.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bandu.tiji.core.storage.db.entity.CollectionEntity
import com.bandu.tiji.core.storage.db.entity.ErrorItemEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ErrorItemFtsInstrumentedTest {
    private lateinit var database: LearningDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LearningDatabase::class.java,
        ).build()
        runBlocking {
            database.collectionDao().insert(CollectionEntity("collection-1", "数学", 1, 1))
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insert_populatesFtsFromAllSearchableColumns(): Unit = runBlocking {
        val item = errorItem()
        database.errorItemDao().insert(item)

        assertThat(database.errorItemDao().search("quadratic")).containsExactly(item)
        assertThat(database.errorItemDao().search("solution")).containsExactly(item)
        assertThat(database.errorItemDao().search("derivation")).containsExactly(item)
        assertThat(database.errorItemDao().search("mistaken")).containsExactly(item)
        assertThat(database.errorItemDao().search("sign")).containsExactly(item)
        assertThat(database.errorItemDao().search("review")).containsExactly(item)
    }

    @Test
    fun update_replacesOldFtsTokens(): Unit = runBlocking {
        val item = errorItem()
        database.errorItemDao().insert(item)
        val updated = item.copy(
            questionText = "parabola vertex",
            answerText = "vertex form",
            updatedAt = 2,
        )

        database.errorItemDao().update(updated)

        assertThat(database.errorItemDao().search("quadratic")).isEmpty()
        assertThat(database.errorItemDao().search("parabola")).containsExactly(updated)
    }

    @Test
    fun delete_removesFtsEntry(): Unit = runBlocking {
        val item = errorItem()
        database.errorItemDao().insert(item)

        database.errorItemDao().delete(item)

        assertThat(database.errorItemDao().search("quadratic")).isEmpty()
    }

    private fun errorItem() = ErrorItemEntity(
        id = "error-1",
        collectionId = "collection-1",
        imagePath = null,
        imageSha256 = null,
        imageWidth = null,
        imageHeight = null,
        questionText = "quadratic equation",
        answerText = "solution set",
        analysis = "complete derivation",
        wrongAnswerText = "mistaken root",
        mistakeStatus = "WRONG_ATTEMPT",
        mistakeAnalysis = "sign error",
        subject = "数学",
        gradeSemester = null,
        paperLevel = null,
        notes = "review tomorrow",
        masteryLevel = 0,
        createdAt = 1,
        updatedAt = 1,
    )
}
