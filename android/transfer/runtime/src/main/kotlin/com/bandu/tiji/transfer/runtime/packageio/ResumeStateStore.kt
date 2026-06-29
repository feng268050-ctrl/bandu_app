package com.bandu.tiji.transfer.runtime.packageio

import com.bandu.tiji.core.common.time.Clock
import com.bandu.tiji.core.common.time.SystemClock
import com.bandu.tiji.transfer.protocol.packageio.PackageIntegrityException
import com.bandu.tiji.transfer.protocol.proto.ChunkResumeState
import com.bandu.tiji.transfer.runtime.TransferRuntimeConfig
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.security.MessageDigest

class ResumeStateStore(
    private val transferRoot: File,
    private val clock: Clock = SystemClock(),
    private val lifetimeMillis: Long = TransferRuntimeConfig.RESUME_LIFETIME_MILLIS,
) {
    fun save(
        sessionId: String,
        manifestSha256: ByteArray,
        states: List<ChunkResumeState>,
    ) {
        require(sessionId.isNotBlank())
        require(manifestSha256.size == SHA256_BYTES)
        val directory = File(transferRoot, sessionId)
        require(directory.exists() || directory.mkdirs()) { "Unable to create resume directory" }
        val target = File(directory, RESUME_FILE)
        val temp = File(directory, "$RESUME_FILE.tmp")
        temp.writeBytes(encode(clock.nowEpochMillis(), manifestSha256, states))
        if (target.exists() && !target.delete()) {
            temp.delete()
            error("Unable to replace resume state")
        }
        if (!temp.renameTo(target)) {
            temp.delete()
            error("Unable to commit resume state")
        }
    }

    fun load(
        sessionId: String,
        manifestSha256: ByteArray,
    ): List<ChunkResumeState>? {
        require(manifestSha256.size == SHA256_BYTES)
        val file = File(File(transferRoot, sessionId), RESUME_FILE)
        if (!file.isFile) return null
        val decoded = runCatching { decode(file.readBytes()) }.getOrElse {
            file.delete()
            throw PackageIntegrityException(cause = it)
        }
        val expired = clock.nowEpochMillis() - decoded.createdAtEpochMillis > lifetimeMillis
        val changed = !MessageDigest.isEqual(decoded.manifestSha256, manifestSha256)
        if (expired || changed) {
            file.delete()
            return null
        }
        return decoded.states
    }

    fun clear(sessionId: String) {
        File(File(transferRoot, sessionId), RESUME_FILE).delete()
    }

    private fun encode(
        createdAtEpochMillis: Long,
        manifestSha256: ByteArray,
        states: List<ChunkResumeState>,
    ): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { output ->
            output.writeInt(MAGIC)
            output.writeInt(VERSION)
            output.writeLong(createdAtEpochMillis)
            output.writeInt(manifestSha256.size)
            output.write(manifestSha256)
            output.writeInt(states.size)
            states.forEach { state ->
                val encoded = state.toByteArray()
                output.writeInt(encoded.size)
                output.write(encoded)
            }
        }
        bytes.toByteArray()
    }

    private fun decode(bytes: ByteArray): ResumeFile =
        DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            if (input.readInt() != MAGIC || input.readInt() != VERSION) {
                throw PackageIntegrityException()
            }
            val createdAt = input.readLong()
            val manifestHashSize = input.readInt()
            if (manifestHashSize != SHA256_BYTES) throw PackageIntegrityException()
            val manifestHash = ByteArray(manifestHashSize).also(input::readFully)
            val count = input.readInt()
            if (count < 0 || count > MAX_RESUME_STATES) throw PackageIntegrityException()
            val states = List(count) {
                val size = input.readInt()
                if (size <= 0 || size > MAX_RESUME_STATE_BYTES) throw PackageIntegrityException()
                ChunkResumeState.parseFrom(ByteArray(size).also(input::readFully))
            }
            ResumeFile(createdAt, manifestHash, states)
        }

    private data class ResumeFile(
        val createdAtEpochMillis: Long,
        val manifestSha256: ByteArray,
        val states: List<ChunkResumeState>,
    )

    companion object {
        const val RESUME_FILE = "resume.pb"
        private const val MAGIC = 0x42545231
        private const val VERSION = 1
        private const val SHA256_BYTES = 32
        private const val MAX_RESUME_STATES = 100_000
        private const val MAX_RESUME_STATE_BYTES = 1024 * 1024
    }
}
