package com.bandu.tiji.core.storage.db

import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bandu.tiji.core.storage.db.entity.CollectionEntity
import com.bandu.tiji.core.storage.db.entity.ErrorItemEntity
import com.bandu.tiji.core.storage.db.entity.ErrorItemTagEntity
import com.bandu.tiji.core.storage.db.entity.TagEntity
import com.bandu.tiji.core.storage.db.projection.ErrorItemSummaryProjection
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ErrorItemPagingDaoInstrumentedTest {
    private lateinit var database: LearningDatabase

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LearningDatabase::class.java,
        ).build()
        seedData()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun emptyFilters_returnsReadOnlySummariesInUpdatedOrder(): Unit = runBlocking {
        val rows = loadPage()

        assertThat(rows.map { it.id }).containsExactly("target", "recent-other", "old").inOrder()
        assertThat(rows.first()).isEqualTo(
            ErrorItemSummaryProjection(
                id = "target",
                collectionId = "collection-math",
                collectionName = "数学",
                imagePath = "images/target.webp",
                questionText = "quadratic target",
                masteryLevel = 1,
                createdAt = 200,
                updatedAt = 500,
            ),
        )
    }

    @Test
    fun keywordFilter_usesFtsProjection(): Unit = runBlocking {
        val rows = loadPage(ftsQuery = "quadratic")

        assertThat(rows.map { it.id }).containsExactly("target", "old").inOrder()
    }

    @Test
    fun keywordAndEveryFilter_returnOnlyCompleteMatch(): Unit = runBlocking {
        val rows = loadPage(
            collectionId = "collection-math",
            ftsQuery = "quadratic",
            masteryLevels = listOf(1),
            createdAfter = 150,
            tagIds = listOf("tag-algebra", "tag-exam"),
            gradeSemester = "高一上",
            paperLevels = listOf("A"),
        )

        assertThat(rows.map { it.id }).containsExactly("target")
    }

    @Test
    fun selectedTags_requireAllTags(): Unit = runBlocking {
        assertThat(loadPage(tagIds = listOf("tag-algebra")).map { it.id })
            .containsExactly("target", "old")
            .inOrder()
        assertThat(loadPage(tagIds = listOf("tag-algebra", "tag-exam")).map { it.id })
            .containsExactly("target")
    }

    private suspend fun loadPage(
        collectionId: String? = null,
        ftsQuery: String? = null,
        masteryLevels: List<Int> = emptyList(),
        createdAfter: Long? = null,
        tagIds: List<String> = emptyList(),
        gradeSemester: String? = null,
        paperLevels: List<String> = emptyList(),
    ): List<ErrorItemSummaryProjection> {
        val source = database.errorItemPagingDao().page(
            collectionId = collectionId,
            ftsQuery = ftsQuery,
            masteryLevels = masteryLevels,
            masteryLevelCount = masteryLevels.size,
            createdAfter = createdAfter,
            tagIds = tagIds,
            tagIdCount = tagIds.size,
            gradeSemester = gradeSemester,
            paperLevels = paperLevels,
            paperLevelCount = paperLevels.size,
        )
        val result = source.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 30,
                placeholdersEnabled = false,
            ),
        )
        return (result as PagingSource.LoadResult.Page).data
    }

    private suspend fun seedData() {
        database.collectionDao().insert(CollectionEntity("collection-math", "数学", 1, 1))
        database.collectionDao().insert(CollectionEntity("collection-physics", "物理", 1, 1))
        database.tagDao().insert(tag("tag-algebra", "代数"))
        database.tagDao().insert(tag("tag-exam", "考试"))

        database.errorItemDao().insert(
            errorItem(
                id = "target",
                collectionId = "collection-math",
                question = "quadratic target",
                mastery = 1,
                createdAt = 200,
                updatedAt = 500,
                gradeSemester = "高一上",
                paperLevel = "A",
            ),
        )
        database.errorItemDao().insert(
            errorItem(
                id = "recent-other",
                collectionId = "collection-physics",
                question = "linear motion",
                mastery = 1,
                createdAt = 250,
                updatedAt = 400,
                gradeSemester = "高一上",
                paperLevel = "A",
            ),
        )
        database.errorItemDao().insert(
            errorItem(
                id = "old",
                collectionId = "collection-math",
                question = "quadratic old",
                mastery = 0,
                createdAt = 50,
                updatedAt = 100,
                gradeSemester = "高一下",
                paperLevel = "B",
            ),
        )
        database.tagDao().link(ErrorItemTagEntity("target", "tag-algebra"))
        database.tagDao().link(ErrorItemTagEntity("target", "tag-exam"))
        database.tagDao().link(ErrorItemTagEntity("old", "tag-algebra"))
    }

    private fun tag(id: String, name: String) = TagEntity(
        id = id,
        name = name,
        subject = "数学",
        parentId = null,
        sortOrder = 0,
        code = null,
        isSystem = false,
        createdAt = 1,
        updatedAt = 1,
    )

    private fun errorItem(
        id: String,
        collectionId: String,
        question: String,
        mastery: Int,
        createdAt: Long,
        updatedAt: Long,
        gradeSemester: String,
        paperLevel: String,
    ) = ErrorItemEntity(
        id = id,
        collectionId = collectionId,
        imagePath = "images/$id.webp",
        imageSha256 = "hash-$id",
        imageWidth = 100,
        imageHeight = 100,
        questionText = question,
        answerText = "answer",
        analysis = "analysis",
        mistakeStatus = "UNKNOWN",
        subject = if (collectionId == "collection-math") "数学" else "物理",
        gradeSemester = gradeSemester,
        paperLevel = paperLevel,
        masteryLevel = mastery,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
