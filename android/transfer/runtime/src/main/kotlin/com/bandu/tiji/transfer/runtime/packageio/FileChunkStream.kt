package com.bandu.tiji.transfer.runtime.packageio

import com.bandu.tiji.transfer.protocol.packageio.ChunkTransferTracker
import com.bandu.tiji.transfer.protocol.packageio.PackageIntegrityException
import com.bandu.tiji.transfer.protocol.proto.DataChunk
import com.bandu.tiji.transfer.protocol.proto.FileEntry
import java.io.File
import java.io.RandomAccessFile

class FileChunkStream(
    private val packageDirectory: File,
    private val chunkSizeBytes: Int = ChunkTransferTracker.CHUNK_SIZE_BYTES,
) {
    init {
        require(chunkSizeBytes == ChunkTransferTracker.CHUNK_SIZE_BYTES)
    }

    fun readChunk(
        entry: FileEntry,
        index: Int,
    ): DataChunk {
        val file = entry.fileUnder(packageDirectory)
        if (!file.isFile || file.length() != entry.size) throw PackageIntegrityException()
        val expectedBytes = expectedChunkBytes(entry, index)
        val buffer = ByteArray(expectedBytes)
        RandomAccessFile(file, "r").use { input ->
            input.seek(index.toLong() * chunkSizeBytes)
            input.readFully(buffer)
        }
        return ChunkTransferTracker.createDataChunk(entry, index, buffer)
    }

    fun readChunks(
        entry: FileEntry,
        indexes: Iterable<Int>,
    ): Sequence<DataChunk> =
        indexes.asSequence().map { index -> readChunk(entry, index) }

    private fun expectedChunkBytes(
        entry: FileEntry,
        index: Int,
    ): Int {
        if (index !in 0 until entry.chunkCount) throw PackageIntegrityException()
        return if (index == entry.chunkCount - 1) {
            val remainder = (entry.size % chunkSizeBytes).toInt()
            if (remainder == 0) chunkSizeBytes else remainder
        } else {
            chunkSizeBytes
        }
    }
}

internal fun FileEntry.fileUnder(packageDirectory: File): File {
    val file = File(packageDirectory, relativePath)
    if (!file.canonicalPath.startsWith(packageDirectory.canonicalPath + File.separator)) {
        throw PackageIntegrityException()
    }
    return file
}
