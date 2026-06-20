package com.bandu.tiji.data.repository

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.bandu.tiji.core.model.enums.MasteryLevel
import com.bandu.tiji.core.model.erroritem.ErrorItemDraft
import com.bandu.tiji.core.model.erroritem.ErrorItemPatch
import com.bandu.tiji.core.model.erroritem.ErrorItemQuery
import com.bandu.tiji.core.model.erroritem.StoredImage
import com.bandu.tiji.core.model.id.CollectionId
import com.bandu.tiji.core.model.id.ErrorItemId
import com.bandu.tiji.core.model.id.TagId
import com.bandu.tiji.core.storage.db.LearningDatabase
import com.bandu.tiji.core.storage.db.entity.CollectionEntity
import com.bandu.tiji.core.storage.db.entity.TagEntity
import com.bandu.tiji.core.testing.id.FixedUuidGenerator
import com.bandu.tiji.core.testing.time.TestClock
import com.bandu.tiji.data.image.ImageCommit
import com.bandu.tiji.data.image.PendingImageCommitter
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
class RoomErrorItemRepositoryTest {
    private lateinit var database: LearningDatabase
    private lateinit var clock: TestClock
    private lateinit var repository: RoomErrorItemRepository
    private lateinit var imageCommitter: RecordingImageCommitter

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LearningDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        database.collectionDao().insert(
            CollectionEntity("collection-1", "默认题集", 100L, 100L),
        )
        database.tagDao().insert(
            TagEntity("tag-1", "代数", "数学", null, 0, null, false, 100L, 100L),
        )
        clock = TestClock(1_000L)
        imageCommitter = RecordingImageCommitter()
        repository = RoomErrorItemRepository(
            database = database,
            uuidGenerator = FixedUuidGenerator("error-1"),
            clock = clock,
            imageCommitter = imageCommitter,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `create observe patch and delete preserve fields and tags`() = runTest {
        repository.observe(ErrorItemId("error-1")).test {
            assertThat(awaitItem()).isNull()

            val id = repository.create(draft())
            assertThat(id).isEqualTo(ErrorItemId("error-1"))
            val created = checkNotNull(awaitItem())
            assertThat(created.tags.map { it.id }).containsExactly(TagId("tag-1"))
            assertThat(created.masteryLevel).isEqualTo(MasteryLevel.NEW)

            clock.setEpochMillis(2_000L)
            repository.update(
                id,
                ErrorItemPatch(
                    answerText = "4",
                    notes = "已订正",
                    masteryLevel = MasteryLevel.MASTERED,
                    tagIds = emptyList(),
                ),
            )
            val updated = checkNotNull(awaitItem())
            assertThat(updated.answerText).isEqualTo("4")
            assertThat(updated.notes).isEqualTo("已订正")
            assertThat(updated.masteryLevel).isEqualTo(MasteryLevel.MASTERED)
            assertThat(updated.tags).isEmpty()
            assertThat(updated.updatedAtEpochMillis).isEqualTo(2_000L)

            repository.delete(setOf(id))
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `paging applies collection keyword mastery and tag filters`() = runTest {
        val id = repository.create(draft())
        repository.update(id, ErrorItemPatch(masteryLevel = MasteryLevel.REVIEWING))

        val result = repository.page(
            ErrorItemQuery(
                collectionId = CollectionId("collection-1"),
                keyword = "求解",
                masteryLevels = setOf(MasteryLevel.REVIEWING),
                tagIds = setOf(TagId("tag-1")),
            ),
        ).load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 20,
                placeholdersEnabled = false,
            ),
        )

        val page = result as PagingSource.LoadResult.Page
        assertThat(page.data).hasSize(1)
        assertThat(page.data.single().id).isEqualTo(id)
        assertThat(page.data.single().tags.map { it.id }).containsExactly(TagId("tag-1"))
    }

    @Test
    fun `batch delete ignores missing ids and removes existing rows atomically`() = runTest {
        val id = repository.create(draft())

        repository.delete(setOf(id, ErrorItemId("missing")))

        assertThat(database.errorItemDao().count()).isEqualTo(0)
        assertThat(database.tagDao().linkCount()).isEqualTo(0)
    }

    @Test
    fun `database failure rolls back an image committed before the transaction`() = runTest {
        val pendingImage = StoredImage(
            relativePath = "temp/capture/draft.jpg",
            sha256Hex = "abc",
            width = 100,
            height = 80,
        )
        imageCommitter.nextImage = pendingImage.copy(relativePath = "images/error-1.jpg")

        val error = runCatching {
            repository.create(
                draft().copy(
                    collectionId = CollectionId("missing-collection"),
                    image = pendingImage,
                ),
            )
        }.exceptionOrNull()

        assertThat(error).isNotNull()
        assertThat(imageCommitter.committedImages).containsExactly(pendingImage)
        assertThat(imageCommitter.rolledBackImages)
            .containsExactly(pendingImage.copy(relativePath = "images/error-1.jpg"))
        assertThat(database.errorItemDao().count()).isEqualTo(0)
    }

    private fun draft(): ErrorItemDraft =
        ErrorItemDraft(
            collectionId = CollectionId("collection-1"),
            image = null,
            questionText = "求解二次方程 x² - 3x + 2 = 0",
            answerText = "x = 1 或 x = 2",
            analysis = "因式分解二次方程",
            subject = "数学",
            tagIds = listOf(TagId("tag-1")),
        )

    private class RecordingImageCommitter : PendingImageCommitter {
        val committedImages = mutableListOf<StoredImage?>()
        val rolledBackImages = mutableListOf<StoredImage?>()
        var nextImage: StoredImage? = null

        override fun commit(
            image: StoredImage?,
            errorItemId: ErrorItemId,
        ): ImageCommit {
            committedImages += image
            return ImageCommit(nextImage ?: image)
        }

        override fun rollback(commit: ImageCommit) {
            rolledBackImages += commit.image
        }
    }
}
