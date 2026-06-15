package com.bandu.tiji.transfer.protocol.packageio

import com.bandu.tiji.transfer.protocol.proto.ChunkAck
import com.bandu.tiji.transfer.protocol.proto.ChunkAckStatus
import com.bandu.tiji.transfer.protocol.proto.ChunkRequest
import com.bandu.tiji.transfer.protocol.proto.ChunkResumeState
import com.bandu.tiji.transfer.protocol.proto.DataChunk
import com.bandu.tiji.transfer.protocol.proto.FileEntry
import com.google.protobuf.ByteString
import java.security.MessageDigest
import java.util.BitSet

class ChunkTransferTracker private constructor(
    private val sessionId: String,
    private val entry: FileEntry,
    private val received: BitSet,
) {
    val acknowledgedCount: Int
        get() = received.cardinality()

    val isComplete: Boolean
        get() = acknowledgedCount == entry.chunkCount

    fun accept(chunk: DataChunk): ChunkAck {
        val index = chunk.index
        val status = when {
            chunk.relativePath != entry.relativePath ||
                index < 0 ||
                index >= entry.chunkCount ||
                chunk.data.size() != expectedChunkBytes(entry, index) ->
                ChunkAckStatus.CHUNK_ACK_STATUS_OUT_OF_RANGE
            chunk.sha256.size() != SHA256_BYTES ||
                !MessageDigest.isEqual(
                    chunk.sha256.toByteArray(),
                    MessageDigest.getInstance("SHA-256").digest(chunk.data.toByteArray()),
                ) -> ChunkAckStatus.CHUNK_ACK_STATUS_HASH_MISMATCH
            else -> {
                received.set(index)
                ChunkAckStatus.CHUNK_ACK_STATUS_ACCEPTED
            }
        }
        return ChunkAck.newBuilder()
            .setRelativePath(chunk.relativePath)
            .setIndex(index)
            .setStatus(status)
            .build()
    }

    fun missingRequest(maxIndexes: Int = Int.MAX_VALUE): ChunkRequest {
        require(maxIndexes > 0) { "maxIndexes must be positive" }
        val request = ChunkRequest.newBuilder()
            .setSessionId(sessionId)
            .setRelativePath(entry.relativePath)
        var index = received.nextClearBit(0)
        var added = 0
        while (index < entry.chunkCount && added < maxIndexes) {
            request.addMissingIndexes(index)
            added += 1
            index = received.nextClearBit(index + 1)
        }
        return request.build()
    }

    fun snapshot(): ChunkResumeState =
        ChunkResumeState.newBuilder()
            .setSessionId(sessionId)
            .setRelativePath(entry.relativePath)
            .setChunkCount(entry.chunkCount)
            .setReceivedBitmap(ByteString.copyFrom(received.toByteArray()))
            .setFileSha256(entry.sha256)
            .build()

    companion object {
        const val CHUNK_SIZE_BYTES = 1024 * 1024
        private const val SHA256_BYTES = 32

        fun start(
            sessionId: String,
            entry: FileEntry,
        ): ChunkTransferTracker {
            validate(sessionId, entry)
            return ChunkTransferTracker(sessionId, entry, BitSet(entry.chunkCount))
        }

        fun restore(
            sessionId: String,
            entry: FileEntry,
            snapshot: ChunkResumeState,
        ): ChunkTransferTracker {
            validate(sessionId, entry)
            val received = BitSet.valueOf(snapshot.receivedBitmap.toByteArray())
            if (snapshot.sessionId != sessionId ||
                snapshot.relativePath != entry.relativePath ||
                snapshot.chunkCount != entry.chunkCount ||
                !MessageDigest.isEqual(
                    snapshot.fileSha256.toByteArray(),
                    entry.sha256.toByteArray(),
                ) ||
                received.length() > entry.chunkCount
            ) {
                throw PackageIntegrityException()
            }
            return ChunkTransferTracker(sessionId, entry, received)
        }

        fun createDataChunk(
            entry: FileEntry,
            index: Int,
            data: ByteArray,
        ): DataChunk {
            validateEntry(entry)
            if (index !in 0 until entry.chunkCount ||
                data.size != expectedChunkBytes(entry, index)
            ) {
                throw PackageIntegrityException()
            }
            return DataChunk.newBuilder()
                .setRelativePath(entry.relativePath)
                .setIndex(index)
                .setData(ByteString.copyFrom(data))
                .setSha256(
                    ByteString.copyFrom(
                        MessageDigest.getInstance("SHA-256").digest(data),
                    ),
                )
                .build()
        }

        private fun validate(sessionId: String, entry: FileEntry) {
            if (sessionId.isBlank()) throw PackageIntegrityException()
            validateEntry(entry)
        }

        private fun validateEntry(entry: FileEntry) {
            TransferManifestIntegrity.requireSafeRelativePath(entry.relativePath)
            if (entry.chunkSize != CHUNK_SIZE_BYTES ||
                entry.sha256.size() != SHA256_BYTES ||
                entry.chunkCount != expectedChunkCount(entry.size)
            ) {
                throw PackageIntegrityException()
            }
        }

        private fun expectedChunkCount(size: Long): Int {
            val count = size / CHUNK_SIZE_BYTES +
                if (size % CHUNK_SIZE_BYTES == 0L) 0 else 1
            if (count > Int.MAX_VALUE) throw PackageIntegrityException()
            return count.toInt()
        }

        private fun expectedChunkBytes(entry: FileEntry, index: Int): Int =
            if (index == entry.chunkCount - 1) {
                val remainder = (entry.size % CHUNK_SIZE_BYTES).toInt()
                if (remainder == 0) CHUNK_SIZE_BYTES else remainder
            } else {
                CHUNK_SIZE_BYTES
            }
    }
}
