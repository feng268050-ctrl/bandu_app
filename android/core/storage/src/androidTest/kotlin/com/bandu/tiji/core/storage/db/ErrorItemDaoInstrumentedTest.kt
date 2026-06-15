package com.bandu.tiji.core.storage.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bandu.tiji.core.storage.db.entity.CollectionEntity
import com.bandu.tiji.core.storage.db.entity.ErrorItemEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ErrorItemDaoInstrumentedTest {
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
    fun crud_persistsAllFieldsAndOrdersByUpdatedAt() = runBlocking {
        val collection = collection("collection-1")
        database.collectionDao().insert(collection)
        val older = errorItem(id = "error-1", collectionId = collection.id, updatedAt = 100)
        val newer = errorItem(id = "error-2", collectionId = collection.id, updatedAt = 200)
        val dao = database.errorItemDao()

        dao.insert(older)
        dao.insert(newer)

        assertThat(dao.getById(older.id)).isEqualTo(older)
        assertThat(dao.observeByCollection(collection.id).first())
            .containsExactly(newer, older)
            .inOrder()

        val updated = older.copy(
            questionText = "更新后的题目",
            answerText = "更新后的答案",
            analysis = "更新后的解析",
            masteryLevel = 2,
            updatedAt = 300,
        )
        assertThat(dao.update(updated)).isEqualTo(1)
        assertThat(dao.getById(older.id)).isEqualTo(updated)

        assertThat(dao.delete(updated)).isEqualTo(1)
        assertThat(dao.getById(updated.id)).isNull()
        assertThat(dao.count()).isEqualTo(1)
    }

    @Test
    fun deletingCollection_cascadesToItsErrorItemsOnly() = runBlocking {
        val firstCollection = collection("collection-1")
        val secondCollection = collection("collection-2")
        database.collectionDao().insert(firstCollection)
        database.collectionDao().insert(secondCollection)
        database.errorItemDao().insert(errorItem("error-1", firstCollection.id))
        database.errorItemDao().insert(errorItem("error-2", secondCollection.id))

        database.collectionDao().delete(firstCollection)

        assertThat(database.errorItemDao().getById("error-1")).isNull()
        assertThat(database.errorItemDao().getById("error-2")).isNotNull()
        assertThat(database.errorItemDao().count()).isEqualTo(1)
    }

    private fun collection(id: String) = CollectionEntity(
        id = id,
        name = id,
        createdAt = 10,
        updatedAt = 10,
    )

    private fun errorItem(
        id: String,
        collectionId: String,
        updatedAt: Long = 100,
    ) = ErrorItemEntity(
        id = id,
        collectionId = collectionId,
        imagePath = "images/$id.webp",
        imageSha256 = "0123456789abcdef",
        imageWidth = 1200,
        imageHeight = 900,
        questionText = "题目 $id",
        answerText = "答案 $id",
        analysis = "解析 $id",
        wrongAnswerText = "错误答案",
        mistakeStatus = "WRONG_ATTEMPT",
        mistakeAnalysis = "错误分析",
        subject = "数学",
        gradeSemester = "高一上",
        paperLevel = "A",
        notes = "笔记",
        masteryLevel = 1,
        createdAt = 50,
        updatedAt = updatedAt,
    )
}
