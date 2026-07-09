package com.bandu.tiji.core.storage.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LearningDatabaseMigrationInstrumentedTest {
    @Test
    fun schemaVersionOne_migratesToCurrentWithoutDestructiveFallback() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val databaseName = "migration-v1.db"
        val helper = MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            LearningDatabase::class.java,
        )
        helper.createDatabase(databaseName, 1).close()

        val databaseFile = context.getDatabasePath(databaseName)
        val database = LearningDatabaseFactory.open(context, databaseFile)
        try {
            assertThat(database.openHelper.readableDatabase.version).isEqualTo(2)
            assertThat(database.collectionDao().observeAll().first()).isEmpty()
            assertThat(database.questionBankDao().observeBankSummaries().first()).isEmpty()
        } finally {
            database.close()
        }

        deleteDatabaseFiles(databaseFile)
    }

    private fun deleteDatabaseFiles(databaseFile: File) {
        listOf(
            databaseFile,
            File("${databaseFile.path}-shm"),
            File("${databaseFile.path}-wal"),
        ).forEach(File::delete)
    }
}
