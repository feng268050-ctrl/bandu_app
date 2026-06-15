package com.bandu.tiji.transfer.protocol.packageio

import com.bandu.tiji.transfer.protocol.proto.FileEntry
import com.bandu.tiji.transfer.protocol.proto.Manifest
import com.bandu.tiji.transfer.protocol.proto.RecordCounts
import com.google.protobuf.ByteString
import java.io.InputStream
import java.security.MessageDigest

object TransferManifestIntegrity {
    const val DEFAULT_CHUNK_SIZE = 1024 * 1024

    fun createFileEntry(
        relativePath: String,
        input: InputStream,
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
    ): FileEntry {
        requireSafeRelativePath(relativePath)
        require(chunkSize > 0) { "Chunk size must be positive" }
        val digest = digest(input)
        val chunkCount = chunkCount(digest.size, chunkSize)
        return FileEntry.newBuilder()
            .setRelativePath(relativePath)
            .setSize(digest.size)
            .setSha256(ByteString.copyFrom(digest.sha256))
            .setChunkSize(chunkSize)
            .setChunkCount(chunkCount)
            .build()
    }

    fun createManifest(
        schemaVersion: Int,
        exportId: String,
        files: List<FileEntry>,
        counts: RecordCounts,
        recordsSha256: ByteArray,
    ): Manifest {
        require(schemaVersion > 0) { "Schema version must be positive" }
        require(exportId.isNotBlank()) { "Export ID must not be blank" }
        requireSha256(recordsSha256)
        validateEntries(files)
        return Manifest.newBuilder()
            .setSchemaVersion(schemaVersion)
            .setExportId(exportId)
            .addAllFiles(files)
            .setCounts(counts)
            .setRecordsSha256(ByteString.copyFrom(recordsSha256))
            .build()
    }

    fun manifestSha256(manifest: Manifest): ByteArray {
        validate(manifest)
        return MessageDigest.getInstance("SHA-256").digest(manifest.toByteArray())
    }

    fun verifyFiles(
        manifest: Manifest,
        sources: Map<String, () -> InputStream>,
    ) {
        validate(manifest)
        manifest.filesList.forEach { entry ->
            val open = sources[entry.relativePath] ?: throw PackageIntegrityException()
            val actual = try {
                open().use(::digest)
            } catch (error: PackageIntegrityException) {
                throw error
            } catch (error: Exception) {
                throw PackageIntegrityException(cause = error)
            }
            if (actual.size != entry.size ||
                !MessageDigest.isEqual(actual.sha256, entry.sha256.toByteArray())
            ) {
                throw PackageIntegrityException()
            }
        }
    }

    fun validate(manifest: Manifest) {
        if (manifest.schemaVersion == 0 || manifest.exportId.isBlank()) {
            throw PackageIntegrityException()
        }
        requireSha256(manifest.recordsSha256.toByteArray())
        validateEntries(manifest.filesList)
    }

    fun requireSafeRelativePath(relativePath: String) {
        val segments = relativePath.split('/')
        if (relativePath.isBlank() ||
            relativePath.startsWith('/') ||
            '\\' in relativePath ||
            '\u0000' in relativePath ||
            segments.any { it.isBlank() || it == "." || it == ".." }
        ) {
            throw PackageIntegrityException()
        }
    }

    private fun validateEntries(files: List<FileEntry>) {
        val paths = HashSet<String>(files.size)
        files.forEach { entry ->
            requireSafeRelativePath(entry.relativePath)
            requireSha256(entry.sha256.toByteArray())
            if (!paths.add(entry.relativePath) ||
                entry.chunkSize == 0 ||
                entry.chunkCount != chunkCount(entry.size, entry.chunkSize)
            ) {
                throw PackageIntegrityException()
            }
        }
    }

    private fun digest(input: InputStream): FileDigest {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_CHUNK_SIZE)
        var size = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count == 0) continue
            messageDigest.update(buffer, 0, count)
            size = Math.addExact(size, count.toLong())
        }
        return FileDigest(size, messageDigest.digest())
    }

    private fun chunkCount(size: Long, chunkSize: Int): Int {
        val count = size / chunkSize + if (size % chunkSize == 0L) 0 else 1
        if (count > Int.MAX_VALUE) throw PackageIntegrityException()
        return count.toInt()
    }

    private data class FileDigest(
        val size: Long,
        val sha256: ByteArray,
    )
}
