package com.bandu.tiji.transfer.runtime.packageio

import com.bandu.tiji.transfer.protocol.packageio.ChunkTransferTracker
import com.bandu.tiji.transfer.protocol.packageio.PortableRecordStream
import com.bandu.tiji.transfer.protocol.proto.CollectionRecord
import com.bandu.tiji.transfer.protocol.proto.PortableRecord
import com.bandu.tiji.transfer.runtime.TransferRuntimeConfig
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.io.File
import kotlin.system.measureTimeMillis
import org.junit.Test

class TransferPackageBenchmarkTest {
    @Test
    fun `exports and verifies five thousand records with chunked images`() {
        val root = File("build/test-transfer-benchmark/${System.nanoTime()}")
        val imageBytes = ByteArray(TransferRuntimeConfig.CHUNK_SIZE_BYTES * 2 + 31) { index ->
            (index % 251).toByte()
        }
        val exporter = TransferPackageExporter(root)

        lateinit var snapshot: TransferPackageSnapshot
        val elapsedMillis = measureTimeMillis {
            snapshot = exporter.export(
                sessionId = "session-5000",
                records = records(count = 5_000),
                files = listOf(
                    TransferFileSource("images/large.jpg") {
                        ByteArrayInputStream(imageBytes)
                    },
                ),
            )
            exporter.verify(snapshot)
        }

        assertThat(elapsedMillis).isLessThan(10_000)
        assertThat(snapshot.totalRecords).isEqualTo(5_000)
        assertThat(snapshot.manifest.counts.collections).isEqualTo(5_000)
        val entry = snapshot.manifest.filesList.single()
        assertThat(entry.chunkSize).isEqualTo(ChunkTransferTracker.CHUNK_SIZE_BYTES)
        assertThat(entry.chunkCount).isEqualTo(3)

        val chunkStream = FileChunkStream(snapshot.directory)
        val firstChunk = chunkStream.readChunk(entry, 0)
        val lastChunk = chunkStream.readChunk(entry, 2)

        assertThat(firstChunk.data.size()).isEqualTo(TransferRuntimeConfig.CHUNK_SIZE_BYTES)
        assertThat(lastChunk.data.size()).isEqualTo(31)

        PortableRecordStream.verify(
            input = snapshot.recordsFile.inputStream(),
            expectedSha256 = snapshot.manifest.recordsSha256.toByteArray(),
            expectedCounts = snapshot.manifest.counts,
        )
    }

    private fun records(count: Int): List<PortableRecord> =
        (0 until count).map { index ->
            PortableRecord.newBuilder()
                .setCollection(
                    CollectionRecord.newBuilder()
                        .setId("collection-$index")
                        .setName("题集 $index")
                        .setCreatedAtEpochMs(index.toLong())
                        .setUpdatedAtEpochMs(index.toLong()),
                )
                .build()
        }
}
