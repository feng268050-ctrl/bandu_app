package com.bandu.tiji.transfer.protocol.packageio

import com.bandu.tiji.transfer.protocol.proto.ChunkAckStatus
import com.bandu.tiji.transfer.protocol.proto.FileEntry
import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import org.junit.Assert.assertThrows
import org.junit.Test

class ChunkTransferTrackerTest {
    @Test
    fun `forty percent interruption resumes with only missing chunks`() {
        val entry = entry(size = 10L * ChunkTransferTracker.CHUNK_SIZE_BYTES)
        val tracker = ChunkTransferTracker.start("session", entry)

        repeat(4) { index ->
            val ack = tracker.accept(
                ChunkTransferTracker.createDataChunk(
                    entry,
                    index,
                    ByteArray(ChunkTransferTracker.CHUNK_SIZE_BYTES) { index.toByte() },
                ),
            )
            assertThat(ack.status).isEqualTo(ChunkAckStatus.CHUNK_ACK_STATUS_ACCEPTED)
        }

        val resumed = ChunkTransferTracker.restore("session", entry, tracker.snapshot())

        assertThat(resumed.acknowledgedCount).isEqualTo(4)
        assertThat(resumed.isComplete).isFalse()
        assertThat(resumed.missingRequest().missingIndexesList)
            .containsExactly(4, 5, 6, 7, 8, 9)
            .inOrder()
    }

    @Test
    fun `ack reports hash mismatch and out of range without advancing bitmap`() {
        val entry = entry(size = ChunkTransferTracker.CHUNK_SIZE_BYTES.toLong())
        val tracker = ChunkTransferTracker.start("session", entry)
        val valid = ChunkTransferTracker.createDataChunk(
            entry,
            0,
            ByteArray(ChunkTransferTracker.CHUNK_SIZE_BYTES),
        )
        val badHash = valid.toBuilder()
            .setSha256(ByteString.copyFrom(ByteArray(32) { 1 }))
            .build()

        assertThat(tracker.accept(badHash).status)
            .isEqualTo(ChunkAckStatus.CHUNK_ACK_STATUS_HASH_MISMATCH)
        assertThat(
            tracker.accept(valid.toBuilder().setIndex(1).build()).status,
        ).isEqualTo(ChunkAckStatus.CHUNK_ACK_STATUS_OUT_OF_RANGE)
        assertThat(tracker.acknowledgedCount).isEqualTo(0)
    }

    @Test
    fun `last chunk uses exact remaining size and duplicate ack is idempotent`() {
        val entry = entry(size = ChunkTransferTracker.CHUNK_SIZE_BYTES + 7L)
        val tracker = ChunkTransferTracker.start("session", entry)
        val last = ChunkTransferTracker.createDataChunk(entry, 1, ByteArray(7))

        assertThat(tracker.accept(last).status)
            .isEqualTo(ChunkAckStatus.CHUNK_ACK_STATUS_ACCEPTED)
        assertThat(tracker.accept(last).status)
            .isEqualTo(ChunkAckStatus.CHUNK_ACK_STATUS_ACCEPTED)
        assertThat(tracker.acknowledgedCount).isEqualTo(1)
        assertThat(tracker.missingRequest().missingIndexesList).containsExactly(0)

        assertThrows(PackageIntegrityException::class.java) {
            ChunkTransferTracker.createDataChunk(entry, 1, ByteArray(8))
        }
    }

    @Test
    fun `resume snapshot is bound to session path chunk count and file hash`() {
        val entry = entry(size = ChunkTransferTracker.CHUNK_SIZE_BYTES.toLong())
        val snapshot = ChunkTransferTracker.start("session", entry).snapshot()
        val changedEntry = entry.toBuilder()
            .setSha256(ByteString.copyFrom(ByteArray(32) { 9 }))
            .build()

        assertThrows(PackageIntegrityException::class.java) {
            ChunkTransferTracker.restore("session", changedEntry, snapshot)
        }
        assertThrows(PackageIntegrityException::class.java) {
            ChunkTransferTracker.restore("different-session", entry, snapshot)
        }
    }

    private fun entry(size: Long): FileEntry {
        val chunkCount = (size / ChunkTransferTracker.CHUNK_SIZE_BYTES +
            if (size % ChunkTransferTracker.CHUNK_SIZE_BYTES == 0L) 0 else 1).toInt()
        return FileEntry.newBuilder()
            .setRelativePath("images/a.jpg")
            .setSize(size)
            .setSha256(ByteString.copyFrom(ByteArray(32) { 7 }))
            .setChunkSize(ChunkTransferTracker.CHUNK_SIZE_BYTES)
            .setChunkCount(chunkCount)
            .build()
    }
}
