package com.bandu.tiji.transfer.runtime.packageio

import com.bandu.tiji.core.common.time.Clock
import com.bandu.tiji.transfer.protocol.packageio.ChunkTransferTracker
import com.bandu.tiji.transfer.protocol.packageio.PackageIntegrityException
import com.bandu.tiji.transfer.protocol.packageio.PortableRecordStream
import com.bandu.tiji.transfer.protocol.proto.CollectionRecord
import com.bandu.tiji.transfer.protocol.proto.PortablePreferencesRecord
import com.bandu.tiji.transfer.protocol.proto.PortableRecord
import com.bandu.tiji.transfer.runtime.TransferRuntimeConfig
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.io.File
import org.junit.Test

class TransferPackageRuntimeTest {
    @Test
    fun `export creates stable snapshot manifest and records without device secrets`() {
        val root = File("build/test-transfer-package/${System.nanoTime()}")
        val sourceBytes = "original-image".encodeToByteArray()
        val exporter = TransferPackageExporter(root)

        val snapshot = exporter.export(
            sessionId = "session-a",
            records = records(),
            files = listOf(
                TransferFileSource("images/item-a.jpg") {
                    ByteArrayInputStream(sourceBytes)
                },
            ),
        )

        exporter.verify(snapshot)
        assertThat(snapshot.manifest.exportId).isEqualTo("session-a")
        assertThat(snapshot.manifest.filesList.single().relativePath).isEqualTo("images/item-a.jpg")
        assertThat(snapshot.totalFiles).isEqualTo(1)
        assertThat(snapshot.totalRecords).isEqualTo(2)
        val decodedRecords = mutableListOf<PortableRecord>()
        PortableRecordStream.verify(
            input = snapshot.recordsFile.inputStream(),
            expectedSha256 = snapshot.manifest.recordsSha256.toByteArray(),
            expectedCounts = snapshot.manifest.counts,
            onRecord = decodedRecords::add,
        )
        assertThat(decodedRecords).containsExactlyElementsIn(records()).inOrder()
    }

    @Test
    fun `chunk stream reads only requested one mebibyte chunks`() {
        val root = File("build/test-transfer-chunks/${System.nanoTime()}")
        val bytes = ByteArray(TransferRuntimeConfig.CHUNK_SIZE_BYTES + 17) { index ->
            (index % 251).toByte()
        }
        val snapshot = TransferPackageExporter(root).export(
            sessionId = "session-chunks",
            records = records(),
            files = listOf(TransferFileSource("images/big.jpg") { ByteArrayInputStream(bytes) }),
        )
        val entry = snapshot.manifest.filesList.single()
        val tracker = ChunkTransferTracker.start(snapshot.sessionId, entry)
        val chunkStream = FileChunkStream(snapshot.directory)

        val lastChunk = chunkStream.readChunks(entry, listOf(1)).single()
        val ack = tracker.accept(lastChunk)

        assertThat(lastChunk.index).isEqualTo(1)
        assertThat(lastChunk.data.size()).isEqualTo(17)
        assertThat(ack.status.name).isEqualTo("CHUNK_ACK_STATUS_ACCEPTED")
        assertThat(tracker.missingRequest().missingIndexesList).containsExactly(0)
    }

    @Test(expected = PackageIntegrityException::class)
    fun `chunk stream rejects missing or changed snapshot file`() {
        val root = File("build/test-transfer-chunk-missing/${System.nanoTime()}")
        val snapshot = TransferPackageExporter(root).export(
            sessionId = "session-missing",
            records = records(),
            files = listOf(TransferFileSource("images/a.jpg") { ByteArrayInputStream(byteArrayOf(1)) }),
        )
        File(snapshot.directory, "images/a.jpg").delete()

        FileChunkStream(snapshot.directory).readChunk(snapshot.manifest.filesList.single(), 0)
    }

    @Test
    fun `resume state survives restart and expires after twenty four hours`() {
        val clock = MutableClock(1_000L)
        val root = File("build/test-transfer-resume/${System.nanoTime()}")
        val store = ResumeStateStore(root, clock)
        val manifestHash = ByteArray(32) { 7 }
        val state = com.bandu.tiji.transfer.protocol.proto.ChunkResumeState.newBuilder()
            .setSessionId("session-r")
            .setRelativePath("images/a.jpg")
            .setChunkCount(2)
            .setReceivedBitmap(com.google.protobuf.ByteString.copyFrom(byteArrayOf(1)))
            .setFileSha256(com.google.protobuf.ByteString.copyFrom(ByteArray(32) { 3 }))
            .build()

        store.save("session-r", manifestHash, listOf(state))

        assertThat(store.load("session-r", manifestHash)).containsExactly(state)

        clock.advance(TransferRuntimeConfig.RESUME_LIFETIME_MILLIS + 1)

        assertThat(store.load("session-r", manifestHash)).isNull()
    }

    @Test
    fun `resume state is ignored when manifest hash changes`() {
        val root = File("build/test-transfer-resume-hash/${System.nanoTime()}")
        val store = ResumeStateStore(root, MutableClock(1_000L))
        store.save("session-r", ByteArray(32) { 1 }, emptyList())

        assertThat(store.load("session-r", ByteArray(32) { 2 })).isNull()
    }

    private fun records(): List<PortableRecord> = listOf(
        PortableRecord.newBuilder()
            .setCollection(
                CollectionRecord.newBuilder()
                    .setId("collection-a")
                    .setName("数学"),
            )
            .build(),
        PortableRecord.newBuilder()
            .setPortablePreferences(
                PortablePreferencesRecord.newBuilder()
                    .setStudentNickname("小明")
                    .setProviderDisplayName("Gemini")
                    .setAnalysisModel("gemini")
                    .setTutorModel("gemini"),
            )
            .build(),
    )
}

private class MutableClock(private var now: Long) : Clock {
    override fun nowEpochMillis(): Long = now

    fun advance(millis: Long) {
        now += millis
    }
}
