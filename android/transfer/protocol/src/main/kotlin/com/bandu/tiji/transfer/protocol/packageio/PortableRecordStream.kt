package com.bandu.tiji.transfer.protocol.packageio

import com.bandu.tiji.transfer.protocol.proto.PortableRecord
import com.bandu.tiji.transfer.protocol.proto.ProtocolErrorCode
import com.bandu.tiji.transfer.protocol.proto.RecordCounts
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.DigestInputStream
import java.security.DigestOutputStream
import java.security.MessageDigest

data class PortableRecordStreamSummary(
    val recordCount: Long,
    val counts: RecordCounts,
    val sha256: ByteArray,
)

object PortableRecordStream {
    const val MAX_RECORD_BYTES = 1024 * 1024

    fun write(
        records: Iterable<PortableRecord>,
        output: OutputStream,
    ): PortableRecordStreamSummary {
        val digest = MessageDigest.getInstance("SHA-256")
        val digestOutput = DigestOutputStream(output, digest)
        val dataOutput = DataOutputStream(digestOutput)
        val counts = RecordCounts.newBuilder()
        var recordCount = 0L
        records.forEach { record ->
            require(record.valueCase != PortableRecord.ValueCase.VALUE_NOT_SET) {
                "Portable record value must be set"
            }
            val encoded = record.toByteArray()
            require(encoded.size in 1..MAX_RECORD_BYTES) {
                "Portable record exceeds $MAX_RECORD_BYTES bytes"
            }
            dataOutput.writeInt(encoded.size)
            dataOutput.write(encoded)
            counts.increment(record)
            recordCount += 1
        }
        dataOutput.flush()
        return PortableRecordStreamSummary(recordCount, counts.build(), digest.digest())
    }

    fun verify(
        input: InputStream,
        expectedSha256: ByteArray,
        expectedCounts: RecordCounts,
        onRecord: (PortableRecord) -> Unit = {},
    ): PortableRecordStreamSummary {
        requireSha256(expectedSha256)
        val digest = MessageDigest.getInstance("SHA-256")
        val digestInput = DigestInputStream(input, digest)
        val counts = RecordCounts.newBuilder()
        var recordCount = 0L
        while (true) {
            val length = digestInput.readLength() ?: break
            if (length !in 1..MAX_RECORD_BYTES) {
                throw PackageIntegrityException()
            }
            val encoded = digestInput.readExactly(length)
            val record = try {
                PortableRecord.parseFrom(encoded)
            } catch (error: Exception) {
                throw PackageIntegrityException(cause = error)
            }
            if (record.valueCase == PortableRecord.ValueCase.VALUE_NOT_SET) {
                throw PackageIntegrityException()
            }
            counts.increment(record)
            recordCount += 1
            onRecord(record)
        }
        val actualSha256 = digest.digest()
        val actualCounts = counts.build()
        if (!MessageDigest.isEqual(expectedSha256, actualSha256) ||
            actualCounts != expectedCounts
        ) {
            throw PackageIntegrityException()
        }
        return PortableRecordStreamSummary(recordCount, actualCounts, actualSha256)
    }
}

private fun RecordCounts.Builder.increment(record: PortableRecord) {
    when (record.valueCase) {
        PortableRecord.ValueCase.COLLECTION -> collections = collections + 1
        PortableRecord.ValueCase.ERROR_ITEM -> errorItems = errorItems + 1
        PortableRecord.ValueCase.TAG -> tags = tags + 1
        PortableRecord.ValueCase.ERROR_ITEM_TAG -> errorItemTags = errorItemTags + 1
        PortableRecord.ValueCase.TUTOR_SESSION -> tutorSessions = tutorSessions + 1
        PortableRecord.ValueCase.TUTOR_MESSAGE -> tutorMessages = tutorMessages + 1
        PortableRecord.ValueCase.EXERCISE -> exercises = exercises + 1
        PortableRecord.ValueCase.PORTABLE_PREFERENCES ->
            portablePreferences = portablePreferences + 1
        PortableRecord.ValueCase.VALUE_NOT_SET -> Unit
    }
}

private fun InputStream.readLength(): Int? {
    val first = read()
    if (first == -1) return null
    val remaining = readExactly(3)
    return (first shl 24) or
        ((remaining[0].toInt() and 0xff) shl 16) or
        ((remaining[1].toInt() and 0xff) shl 8) or
        (remaining[2].toInt() and 0xff)
}

internal fun InputStream.readExactly(length: Int): ByteArray {
    val result = ByteArray(length)
    var offset = 0
    while (offset < length) {
        val count = read(result, offset, length - offset)
        if (count < 0) throw PackageIntegrityException()
        if (count == 0) continue
        offset += count
    }
    return result
}

internal fun requireSha256(hash: ByteArray) {
    if (hash.size != 32) throw PackageIntegrityException()
}

class PackageIntegrityException(
    val code: ProtocolErrorCode = ProtocolErrorCode.PROTOCOL_ERROR_CODE_INTEGRITY_FAILED,
    cause: Throwable? = null,
) : SecurityException(code.name, cause)
