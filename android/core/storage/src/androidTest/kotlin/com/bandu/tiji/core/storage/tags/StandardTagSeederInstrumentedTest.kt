package com.bandu.tiji.core.storage.tags

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bandu.tiji.core.storage.db.LearningDatabase
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StandardTagSeederInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private lateinit var database: LearningDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, LearningDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun repeatedStartup_upsertsStableIdsWithoutDuplicates() = runBlocking {
        val reader = StandardTagAssetReader(context)
        val definitions = reader.read()
        val seeder = StandardTagSeeder(database, reader)

        seeder.seed(nowEpochMillis = 100)
        val first = database.tagDao().getSystemTags()
        seeder.seed(nowEpochMillis = 200)
        val second = database.tagDao().getSystemTags()

        assertThat(first).hasSize(definitions.size)
        assertThat(second).hasSize(definitions.size)
        assertThat(second.map { it.id }).containsExactlyElementsIn(first.map { it.id }).inOrder()
        assertThat(second.map { it.code }).containsNoDuplicates()
        assertThat(second.map { it.subject }.toSet()).containsExactly(
            "math",
            "physics",
            "chemistry",
            "biology",
            "english",
            "chinese",
            "history",
            "geography",
            "politics",
        )
        assertThat(second.all { it.isSystem && it.updatedAt == 200L }).isTrue()
    }
}
