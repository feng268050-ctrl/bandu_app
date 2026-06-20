package com.bandu.tiji.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.bandu.tiji.core.model.id.TagId
import com.bandu.tiji.core.storage.db.LearningDatabase
import com.bandu.tiji.core.storage.db.entity.TagEntity
import com.bandu.tiji.core.testing.id.FixedUuidGenerator
import com.bandu.tiji.core.testing.time.TestClock
import com.bandu.tiji.domain.tag.CreateTagInput
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
class RoomTagRepositoryTest {
    private lateinit var database: LearningDatabase
    private lateinit var repository: RoomTagRepository
    private lateinit var clock: TestClock

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LearningDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        clock = TestClock(1_000L)
        repository = RoomTagRepository(
            database = database,
            uuidGenerator = FixedUuidGenerator("custom-1"),
            clock = clock,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `custom tag create rename and delete update tree`() = runTest {
        database.tagDao().insert(systemTag("system-root"))

        repository.observeTree("数学").test {
            assertThat(awaitItem().single().tag.id).isEqualTo(TagId("system-root"))

            val id = repository.createCustom(
                CreateTagInput("函数", "数学", TagId("system-root")),
            )
            assertThat(awaitItem().single().children.single().tag.id).isEqualTo(id)

            clock.setEpochMillis(2_000L)
            repository.renameCustom(id, "一次函数")
            assertThat(awaitItem().single().children.single().tag.name).isEqualTo("一次函数")
            assertThat(database.tagDao().getById(id.value)?.updatedAt).isEqualTo(2_000L)

            repository.deleteCustom(id)
            assertThat(awaitItem().single().children).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `system tags reject rename and delete`() = runTest {
        database.tagDao().insert(systemTag("system-root"))

        val renameError = runCatching {
            repository.renameCustom(TagId("system-root"), "新名称")
        }.exceptionOrNull()
        val deleteError = runCatching {
            repository.deleteCustom(TagId("system-root"))
        }.exceptionOrNull()

        assertThat(renameError).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(deleteError).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(database.tagDao().getById("system-root")).isNotNull()
    }

    @Test
    fun `duplicate custom names and cross-subject parents are rejected`() = runTest {
        database.tagDao().insert(systemTag("system-root"))
        database.tagDao().insert(
            TagEntity("existing", "函数", "数学", "system-root", 0, null, false, 1L, 1L),
        )

        val duplicate = runCatching {
            repository.createCustom(CreateTagInput("函数", "数学", TagId("system-root")))
        }.exceptionOrNull()
        val wrongSubject = runCatching {
            repository.createCustom(CreateTagInput("力学", "物理", TagId("system-root")))
        }.exceptionOrNull()

        assertThat(duplicate).isInstanceOf(IllegalStateException::class.java)
        assertThat(wrongSubject).isInstanceOf(IllegalArgumentException::class.java)
    }

    private fun systemTag(id: String): TagEntity =
        TagEntity(
            id = id,
            name = "数学",
            subject = "数学",
            parentId = null,
            sortOrder = 0,
            code = "MATH",
            isSystem = true,
            createdAt = 1L,
            updatedAt = 1L,
        )
}
