package com.bandu.tiji.core.storage.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bandu.tiji.core.storage.db.entity.CollectionEntity
import com.bandu.tiji.core.storage.db.entity.ErrorItemEntity
import com.bandu.tiji.core.storage.db.entity.ErrorItemTagEntity
import com.bandu.tiji.core.storage.db.entity.TagEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TagDaoInstrumentedTest {
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
    fun associations_queryInOrderAndCascadeFromBothSides() = runBlocking {
        seedErrorItem("error-1")
        val dao = database.tagDao()
        val algebra = tag("tag-algebra", "代数", sortOrder = 20)
        val geometry = tag("tag-geometry", "几何", sortOrder = 10)
        dao.insert(algebra)
        dao.insert(geometry)

        assertThat(dao.link(ErrorItemTagEntity("error-1", algebra.id))).isNotEqualTo(-1)
        assertThat(dao.link(ErrorItemTagEntity("error-1", geometry.id))).isNotEqualTo(-1)
        assertThat(dao.link(ErrorItemTagEntity("error-1", geometry.id))).isEqualTo(-1)
        assertThat(dao.getTagsForErrorItem("error-1"))
            .containsExactly(geometry, algebra)
            .inOrder()

        dao.delete(geometry)
        assertThat(dao.getTagsForErrorItem("error-1")).containsExactly(algebra)
        assertThat(dao.linkCount()).isEqualTo(1)

        database.errorItemDao().delete(database.errorItemDao().getById("error-1")!!)
        assertThat(dao.linkCount()).isEqualTo(0)
        assertThat(dao.getById(algebra.id)).isNotNull()
    }

    @Test
    fun deletingParent_setsChildParentToNull() = runBlocking {
        val dao = database.tagDao()
        val parent = tag("tag-parent", "数学")
        val child = tag("tag-child", "函数", parentId = parent.id)
        dao.insert(parent)
        dao.insert(child)

        dao.delete(parent)

        assertThat(dao.getById(child.id)!!.parentId).isNull()
    }

    @Test
    fun insert_rejectsCaseInsensitiveDuplicateAtRootAndUnderParent() = runBlocking {
        val dao = database.tagDao()
        val parent = tag("tag-parent", "数学")
        dao.insert(parent)
        dao.insert(tag("tag-root-1", "Algebra"))
        dao.insert(tag("tag-child-1", "Geometry", parentId = parent.id))

        val rootFailure = runCatching {
            dao.insert(tag("tag-root-2", "algebra"))
        }.exceptionOrNull()
        val childFailure = runCatching {
            dao.insert(tag("tag-child-2", "geometry", parentId = parent.id))
        }.exceptionOrNull()

        assertThat(rootFailure).isInstanceOf(IllegalStateException::class.java)
        assertThat(childFailure).isInstanceOf(IllegalStateException::class.java)
    }

    private suspend fun seedErrorItem(id: String) {
        database.collectionDao().insert(CollectionEntity("collection-1", "数学", 1, 1))
        database.errorItemDao().insert(
            ErrorItemEntity(
                id = id,
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
            ),
        )
    }

    private fun tag(
        id: String,
        name: String,
        parentId: String? = null,
        sortOrder: Int = 0,
    ) = TagEntity(
        id = id,
        name = name,
        subject = "数学",
        parentId = parentId,
        sortOrder = sortOrder,
        code = null,
        isSystem = false,
        createdAt = 1,
        updatedAt = 1,
    )
}
