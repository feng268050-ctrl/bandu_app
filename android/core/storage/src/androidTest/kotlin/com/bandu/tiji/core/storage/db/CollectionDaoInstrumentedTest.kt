package com.bandu.tiji.core.storage.db

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bandu.tiji.core.storage.db.entity.CollectionEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CollectionDaoInstrumentedTest {
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
    fun crud_persistsUpdatesAndDeletesCollection() = runBlocking {
        val dao = database.collectionDao()
        val collection = CollectionEntity(
            id = "collection-1",
            name = "代数",
            createdAt = 100,
            updatedAt = 100,
        )

        dao.insert(collection)
        assertThat(dao.getById(collection.id)).isEqualTo(collection)
        assertThat(dao.observeAll().first()).containsExactly(collection)

        val renamed = collection.copy(name = "高等代数", updatedAt = 200)
        assertThat(dao.update(renamed)).isEqualTo(1)
        assertThat(dao.getById(collection.id)).isEqualTo(renamed)

        assertThat(dao.delete(renamed)).isEqualTo(1)
        assertThat(dao.getById(collection.id)).isNull()
    }

    @Test
    fun insert_rejectsCaseInsensitiveDuplicateName() = runBlocking {
        val dao = database.collectionDao()
        dao.insert(CollectionEntity("collection-1", "Math", 100, 100))

        val failure = runCatching {
            dao.insert(CollectionEntity("collection-2", "math", 200, 200))
        }.exceptionOrNull()

        assertThat(failure).isInstanceOf(SQLiteConstraintException::class.java)
    }
}
