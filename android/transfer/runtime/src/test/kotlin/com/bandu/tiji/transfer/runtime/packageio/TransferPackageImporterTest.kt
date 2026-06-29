package com.bandu.tiji.transfer.runtime.packageio

import com.bandu.tiji.transfer.protocol.proto.CollectionRecord
import com.bandu.tiji.transfer.protocol.proto.ErrorItemRecord
import com.bandu.tiji.transfer.protocol.proto.Manifest
import com.bandu.tiji.transfer.protocol.proto.PortablePreferencesRecord
import com.bandu.tiji.transfer.protocol.proto.PortableRecord
import com.bandu.tiji.transfer.protocol.proto.TagRecord
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TransferPackageImporterTest {
    @Test
    fun `imports records in foreign key order with max five hundred batches`() = runTest {
        val snapshot = snapshot(
            records = buildList {
                repeat(501) { index ->
                    add(record(TagRecord.newBuilder().setId("tag-$index").setName("标签$index").build()))
                    add(record(CollectionRecord.newBuilder().setId("collection-$index").setName("题集$index").build()))
                    add(
                        record(
                            ErrorItemRecord.newBuilder()
                                .setId("item-$index")
                                .setCollectionId("collection-$index")
                                .setQuestionText("Q$index")
                                .build(),
                        ),
                    )
                }
                add(record(PortablePreferencesRecord.newBuilder().setStudentNickname("小明").build()))
            },
        )
        val sink = RecordingImportSink()

        val result = TransferPackageImporter(
            sink = sink,
            healthChecker = PassingHealthChecker,
        ).importVerified(snapshot)

        assertThat(result).isEqualTo(ImportResult.Imported(totalRecords = 1_504, totalFiles = 1))
        assertThat(sink.events.take(6)).containsExactly(
            "collections:500",
            "collections:1",
            "tags:500",
            "tags:1",
            "items:500",
            "items:1",
        ).inOrder()
        assertThat(sink.events.last()).isEqualTo("preferences:true")
    }

    @Test
    fun `rejects package when record stream is tampered`() = runTest {
        val snapshot = snapshot(records = listOf(record(CollectionRecord.newBuilder().setId("c").build())))
        val bytes = snapshot.recordsFile.readBytes()
        snapshot.recordsFile.writeBytes(bytes.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() })
        val sink = RecordingImportSink()

        val result = TransferPackageImporter(sink, PassingHealthChecker).importVerified(snapshot)

        assertThat(result).isInstanceOf(ImportResult.Rejected::class.java)
        assertThat(sink.events).isEmpty()
    }

    @Test
    fun `rejects package when health check fails after import`() = runTest {
        val snapshot = snapshot(records = listOf(record(CollectionRecord.newBuilder().setId("c").build())))
        val sink = RecordingImportSink()

        val result = TransferPackageImporter(
            sink = sink,
            healthChecker = ImportHealthChecker { ImportHealth(ok = false, reason = "foreign key failed") },
        ).importVerified(snapshot)

        assertThat(result).isEqualTo(ImportResult.Rejected("foreign key failed"))
        assertThat(sink.events).isNotEmpty()
    }

    private fun snapshot(records: List<PortableRecord>): TransferPackageSnapshot =
        TransferPackageExporter(File("build/test-transfer-importer/${System.nanoTime()}")).export(
            sessionId = "session-import",
            records = records,
            files = listOf(
                TransferFileSource("images/a.jpg") {
                    ByteArrayInputStream(byteArrayOf(1, 2, 3))
                },
            ),
        )

    private fun record(value: CollectionRecord): PortableRecord =
        PortableRecord.newBuilder().setCollection(value).build()

    private fun record(value: TagRecord): PortableRecord =
        PortableRecord.newBuilder().setTag(value).build()

    private fun record(value: ErrorItemRecord): PortableRecord =
        PortableRecord.newBuilder().setErrorItem(value).build()

    private fun record(value: PortablePreferencesRecord): PortableRecord =
        PortableRecord.newBuilder().setPortablePreferences(value).build()
}

private object PassingHealthChecker : ImportHealthChecker {
    override suspend fun check(manifest: Manifest): ImportHealth = ImportHealth(ok = true)
}

private class RecordingImportSink : PortableRecordImportSink {
    val events = mutableListOf<String>()

    override suspend fun insertCollections(records: List<CollectionRecord>) {
        events += "collections:${records.size}"
    }

    override suspend fun insertTags(records: List<TagRecord>) {
        events += "tags:${records.size}"
    }

    override suspend fun insertErrorItems(records: List<ErrorItemRecord>) {
        events += "items:${records.size}"
    }

    override suspend fun insertErrorItemTags(
        records: List<com.bandu.tiji.transfer.protocol.proto.ErrorItemTagRecord>,
    ) {
        events += "itemTags:${records.size}"
    }

    override suspend fun insertTutorSessions(
        records: List<com.bandu.tiji.transfer.protocol.proto.TutorSessionRecord>,
    ) {
        events += "sessions:${records.size}"
    }

    override suspend fun insertTutorMessages(
        records: List<com.bandu.tiji.transfer.protocol.proto.TutorMessageRecord>,
    ) {
        events += "messages:${records.size}"
    }

    override suspend fun insertExercises(
        records: List<com.bandu.tiji.transfer.protocol.proto.ExerciseRecord>,
    ) {
        events += "exercises:${records.size}"
    }

    override suspend fun replacePortablePreferences(record: PortablePreferencesRecord?) {
        events += "preferences:${record != null}"
    }
}
