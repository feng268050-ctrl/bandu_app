package com.bandu.tiji.core.storage.db.benchmark

import android.os.SystemClock
import androidx.paging.PagingSource
import androidx.room.Room
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bandu.tiji.core.storage.db.LearningDatabase
import com.bandu.tiji.core.storage.db.projection.ErrorItemSummaryProjection
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ErrorItemBenchmarkInstrumentedTest {
    private lateinit var database: LearningDatabase

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LearningDatabase::class.java,
        ).build()
        BenchmarkDataSeeder(database).seed()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun fiveThousandItemsKeepListPagingFilteringAndQueryPlansInBenchmarkEnvelope() = runBlocking {
        assertThat(database.errorItemDao().count()).isEqualTo(BenchmarkDataSeeder.DEFAULT_ITEM_COUNT)
        assertQueryPlanContains(
            sql = "SELECT id FROM error_items ORDER BY updated_at DESC, id LIMIT 30",
            expected = "index_error_items_updated_at_id",
        )
        assertQueryPlanContains(
            sql = """
                SELECT id FROM error_items
                WHERE collection_id = 'collection-03'
                ORDER BY updated_at DESC, id
                LIMIT 30
            """.trimIndent(),
            expected = "index_error_items_collection_id_updated_at",
        )
        assertQueryPlanContains(
            sql = """
                SELECT rowid FROM error_item_fts
                WHERE error_item_fts MATCH 'quadratic'
            """.trimIndent(),
            expected = "VIRTUAL TABLE INDEX",
        )
        assertQueryPlanContains(
            sql = """
                SELECT error_item_id FROM error_item_tags
                WHERE tag_id = 'tag-03'
            """.trimIndent(),
            expected = "index_error_item_tags_tag_id_error_item_id",
        )

        val firstPage = loadTimed(
            source = pageSource(),
            params = PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 30,
                placeholdersEnabled = false,
            ),
        )
        val secondPage = loadTimed(
            source = pageSource(),
            params = PagingSource.LoadParams.Append(
                key = requireNotNull(firstPage.page.nextKey),
                loadSize = 30,
                placeholdersEnabled = false,
            ),
        )
        val filteredPage = loadTimed(
            source = pageSource(collectionId = "collection-05", ftsQuery = "quadratic"),
            params = PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 30,
                placeholdersEnabled = false,
            ),
        )

        assertThat(firstPage.page.data).hasSize(30)
        assertThat(firstPage.page.data.first().id).isEqualTo(BenchmarkDataSeeder.itemId(4_999))
        assertThat(secondPage.page.data).hasSize(30)
        assertThat(secondPage.page.data.first().id).isEqualTo(BenchmarkDataSeeder.itemId(4_969))
        assertThat(filteredPage.page.data).isNotEmpty()
        assertThat(filteredPage.page.data.all { it.collectionId == "collection-05" }).isTrue()
        assertThat(filteredPage.page.data.all { it.questionText.contains("quadratic") }).isTrue()

        assertThat(firstPage.elapsedMillis).isLessThan(2_000L)
        assertThat(secondPage.elapsedMillis).isLessThan(1_000L)
        assertThat(filteredPage.elapsedMillis).isLessThan(2_000L)
    }

    private fun pageSource(
        collectionId: String? = null,
        ftsQuery: String? = null,
    ) = database.errorItemPagingDao().page(
        collectionId = collectionId,
        ftsQuery = ftsQuery,
        masteryLevels = emptyList(),
        masteryLevelCount = 0,
        createdAfter = null,
        tagIds = emptyList(),
        tagIdCount = 0,
        gradeSemester = null,
        paperLevels = emptyList(),
        paperLevelCount = 0,
    )

    private suspend fun loadTimed(
        source: PagingSource<Int, ErrorItemSummaryProjection>,
        params: PagingSource.LoadParams<Int>,
    ): TimedPage {
        val startedAt = SystemClock.elapsedRealtime()
        val result = source.load(params)
        val elapsed = SystemClock.elapsedRealtime() - startedAt
        return TimedPage(
            page = result as PagingSource.LoadResult.Page<Int, ErrorItemSummaryProjection>,
            elapsedMillis = elapsed,
        )
    }

    private fun assertQueryPlanContains(
        sql: String,
        expected: String,
    ) {
        val plan = explain(sql).joinToString(separator = "\n")
        assertThat(plan).contains(expected)
    }

    private fun explain(sql: String): List<String> {
        database.openHelper.readableDatabase.query(SimpleSQLiteQuery("EXPLAIN QUERY PLAN $sql")).use { cursor ->
            val detailColumn = cursor.getColumnIndexOrThrow("detail")
            val details = mutableListOf<String>()
            while (cursor.moveToNext()) {
                details += cursor.getString(detailColumn)
            }
            return details
        }
    }

    private data class TimedPage(
        val page: PagingSource.LoadResult.Page<Int, ErrorItemSummaryProjection>,
        val elapsedMillis: Long,
    )
}
