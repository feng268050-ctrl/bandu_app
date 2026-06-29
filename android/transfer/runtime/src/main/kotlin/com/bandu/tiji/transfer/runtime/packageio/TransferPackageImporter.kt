package com.bandu.tiji.transfer.runtime.packageio

import com.bandu.tiji.transfer.protocol.packageio.PackageIntegrityException
import com.bandu.tiji.transfer.protocol.packageio.PortableRecordStream
import com.bandu.tiji.transfer.protocol.packageio.TransferManifestIntegrity
import com.bandu.tiji.transfer.protocol.proto.CollectionRecord
import com.bandu.tiji.transfer.protocol.proto.ErrorItemRecord
import com.bandu.tiji.transfer.protocol.proto.ErrorItemTagRecord
import com.bandu.tiji.transfer.protocol.proto.ExerciseRecord
import com.bandu.tiji.transfer.protocol.proto.Manifest
import com.bandu.tiji.transfer.protocol.proto.PortablePreferencesRecord
import com.bandu.tiji.transfer.protocol.proto.PortableRecord
import com.bandu.tiji.transfer.protocol.proto.TagRecord
import com.bandu.tiji.transfer.protocol.proto.TutorMessageRecord
import com.bandu.tiji.transfer.protocol.proto.TutorSessionRecord
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

class TransferPackageImporter(
    private val sink: PortableRecordImportSink,
    private val healthChecker: ImportHealthChecker,
    private val batchSize: Int = DEFAULT_BATCH_SIZE,
) {
    init {
        require(batchSize in 1..DEFAULT_BATCH_SIZE)
    }

    suspend fun importVerified(snapshot: TransferPackageSnapshot): ImportResult =
        runCatching {
            val manifest = Manifest.parseFrom(snapshot.manifestFile.readBytes())
            if (!MessageDigest.isEqual(
                    TransferManifestIntegrity.manifestSha256(manifest),
                    snapshot.manifestSha256,
                )
            ) {
                throw PackageIntegrityException()
            }
            val buffers = PortableImportBuffers()
            PortableRecordStream.verify(
                input = FileInputStream(snapshot.recordsFile),
                expectedSha256 = manifest.recordsSha256.toByteArray(),
                expectedCounts = manifest.counts,
                onRecord = buffers::add,
            )
            TransferManifestIntegrity.verifyFiles(
                manifest,
                manifest.filesList.associate { entry ->
                    entry.relativePath to { FileInputStream(File(snapshot.directory, entry.relativePath)) }
                },
            )
            buffers.flushInForeignKeyOrder(sink, batchSize)
            val health = healthChecker.check(manifest)
            if (!health.ok) {
                return ImportResult.Rejected(health.reason)
            }
            ImportResult.Imported(
                totalRecords = snapshot.totalRecords,
                totalFiles = snapshot.totalFiles,
            )
        }.getOrElse { error ->
            ImportResult.Rejected(error.message.orEmpty())
        }

    companion object {
        const val DEFAULT_BATCH_SIZE = 500
    }
}

interface PortableRecordImportSink {
    suspend fun insertCollections(records: List<CollectionRecord>)

    suspend fun insertTags(records: List<TagRecord>)

    suspend fun insertErrorItems(records: List<ErrorItemRecord>)

    suspend fun insertErrorItemTags(records: List<ErrorItemTagRecord>)

    suspend fun insertTutorSessions(records: List<TutorSessionRecord>)

    suspend fun insertTutorMessages(records: List<TutorMessageRecord>)

    suspend fun insertExercises(records: List<ExerciseRecord>)

    suspend fun replacePortablePreferences(record: PortablePreferencesRecord?)
}

fun interface ImportHealthChecker {
    suspend fun check(manifest: Manifest): ImportHealth
}

data class ImportHealth(
    val ok: Boolean,
    val reason: String = "",
)

sealed interface ImportResult {
    data class Imported(
        val totalRecords: Long,
        val totalFiles: Int,
    ) : ImportResult

    data class Rejected(val reason: String) : ImportResult
}

private class PortableImportBuffers {
    private val collections = mutableListOf<CollectionRecord>()
    private val tags = mutableListOf<TagRecord>()
    private val errorItems = mutableListOf<ErrorItemRecord>()
    private val errorItemTags = mutableListOf<ErrorItemTagRecord>()
    private val tutorSessions = mutableListOf<TutorSessionRecord>()
    private val tutorMessages = mutableListOf<TutorMessageRecord>()
    private val exercises = mutableListOf<ExerciseRecord>()
    private var preferences: PortablePreferencesRecord? = null

    fun add(record: PortableRecord) {
        when (record.valueCase) {
            PortableRecord.ValueCase.COLLECTION -> collections += record.collection
            PortableRecord.ValueCase.TAG -> tags += record.tag
            PortableRecord.ValueCase.ERROR_ITEM -> errorItems += record.errorItem
            PortableRecord.ValueCase.ERROR_ITEM_TAG -> errorItemTags += record.errorItemTag
            PortableRecord.ValueCase.TUTOR_SESSION -> tutorSessions += record.tutorSession
            PortableRecord.ValueCase.TUTOR_MESSAGE -> tutorMessages += record.tutorMessage
            PortableRecord.ValueCase.EXERCISE -> exercises += record.exercise
            PortableRecord.ValueCase.PORTABLE_PREFERENCES -> preferences = record.portablePreferences
            PortableRecord.ValueCase.VALUE_NOT_SET -> throw PackageIntegrityException()
        }
    }

    suspend fun flushInForeignKeyOrder(
        sink: PortableRecordImportSink,
        batchSize: Int,
    ) {
        collections.chunked(batchSize).forEach { sink.insertCollections(it) }
        tags.chunked(batchSize).forEach { sink.insertTags(it) }
        errorItems.chunked(batchSize).forEach { sink.insertErrorItems(it) }
        errorItemTags.chunked(batchSize).forEach { sink.insertErrorItemTags(it) }
        tutorSessions.chunked(batchSize).forEach { sink.insertTutorSessions(it) }
        tutorMessages.chunked(batchSize).forEach { sink.insertTutorMessages(it) }
        exercises.chunked(batchSize).forEach { sink.insertExercises(it) }
        sink.replacePortablePreferences(preferences)
    }
}
