package com.bandu.tiji.transfer.runtime.packageio

import com.bandu.tiji.transfer.protocol.packageio.PackageIntegrityException
import com.bandu.tiji.transfer.protocol.packageio.PortableRecordStream
import com.bandu.tiji.transfer.protocol.packageio.TransferManifestIntegrity
import com.bandu.tiji.transfer.protocol.proto.Manifest
import com.bandu.tiji.transfer.protocol.proto.PortableRecord
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

class TransferPackageExporter(
    private val transferRoot: File,
    private val schemaVersion: Int = 1,
) {
    fun export(
        sessionId: String,
        records: Iterable<PortableRecord>,
        files: List<TransferFileSource>,
    ): TransferPackageSnapshot {
        require(sessionId.isNotBlank()) { "Session ID must not be blank" }
        val directory = File(transferRoot, sessionId).also { dir ->
            if (dir.exists()) dir.deleteRecursively()
            require(dir.mkdirs()) { "Unable to create transfer package directory" }
        }
        val recordsFile = File(directory, RECORDS_FILE)
        val recordSummary = FileOutputStream(recordsFile).use { output ->
            PortableRecordStream.write(records, output)
        }
        val copiedFiles = files.map { source ->
            TransferManifestIntegrity.requireSafeRelativePath(source.relativePath)
            val target = File(directory, source.relativePath)
            require(target.canonicalPath.startsWith(directory.canonicalPath + File.separator)) {
                "Unsafe package target path"
            }
            require(target.parentFile?.let { it.exists() || it.mkdirs() } != false) {
                "Unable to create package file directory"
            }
            source.open().use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output, BUFFER_SIZE_BYTES)
                }
            }
            FileInputStream(target).use { input ->
                TransferManifestIntegrity.createFileEntry(
                    relativePath = source.relativePath,
                    input = input,
                )
            }
        }
        val manifest = TransferManifestIntegrity.createManifest(
            schemaVersion = schemaVersion,
            exportId = sessionId,
            files = copiedFiles,
            counts = recordSummary.counts,
            recordsSha256 = recordSummary.sha256,
        )
        val manifestFile = File(directory, MANIFEST_FILE)
        manifestFile.writeBytes(manifest.toByteArray())
        return TransferPackageSnapshot(
            sessionId = sessionId,
            directory = directory,
            manifestFile = manifestFile,
            recordsFile = recordsFile,
            manifest = manifest,
            manifestSha256 = TransferManifestIntegrity.manifestSha256(manifest),
            totalBytes = copiedFiles.sumOf { it.size } + recordsFile.length() + manifestFile.length(),
            totalFiles = copiedFiles.size,
            totalRecords = recordSummary.recordCount,
        )
    }

    fun verify(snapshot: TransferPackageSnapshot) {
        val manifest = Manifest.parseFrom(snapshot.manifestFile.readBytes())
        if (!MessageDigest.isEqual(
                TransferManifestIntegrity.manifestSha256(manifest),
                snapshot.manifestSha256,
            )
        ) {
            throw PackageIntegrityException()
        }
        PortableRecordStream.verify(
            input = FileInputStream(snapshot.recordsFile),
            expectedSha256 = manifest.recordsSha256.toByteArray(),
            expectedCounts = manifest.counts,
        )
        TransferManifestIntegrity.verifyFiles(
            manifest,
            manifest.filesList.associate { entry ->
                entry.relativePath to { FileInputStream(File(snapshot.directory, entry.relativePath)) }
            },
        )
    }

    companion object {
        const val MANIFEST_FILE = "manifest.pb"
        const val RECORDS_FILE = "records.pbstream"
        private const val BUFFER_SIZE_BYTES = 64 * 1024
    }
}

data class TransferFileSource(
    val relativePath: String,
    val open: () -> java.io.InputStream,
)

data class TransferPackageSnapshot(
    val sessionId: String,
    val directory: File,
    val manifestFile: File,
    val recordsFile: File,
    val manifest: Manifest,
    val manifestSha256: ByteArray,
    val totalBytes: Long,
    val totalFiles: Int,
    val totalRecords: Long,
)
