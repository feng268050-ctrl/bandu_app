package com.bandu.tiji.transfer.protocol

import com.bandu.tiji.transfer.protocol.crypto.AesGcmFrameCodec
import com.bandu.tiji.transfer.protocol.crypto.FrameSecurityException
import com.bandu.tiji.transfer.protocol.packageio.ChunkTransferTracker
import com.bandu.tiji.transfer.protocol.proto.ChunkResumeState
import com.bandu.tiji.transfer.protocol.proto.FileEntry
import com.bandu.tiji.transfer.protocol.proto.MessageType
import com.google.common.truth.Truth.assertThat
import com.google.protobuf.ByteString
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.BitSet
import kotlin.random.Random

class ProtocolSecurityPropertyTest {
    @Test
    fun `randomized encrypted frame sequences preserve type payload and sequence`() {
        val random = Random(0x42544a31)
        val (sender, receiver) = codecs()

        repeat(256) { sequence ->
            val type = MESSAGE_TYPES[random.nextInt(MESSAGE_TYPES.size)]
            val payload = random.nextBytes(random.nextInt(0, 8 * 1024))
            val decoded = receiver.decode(sender.encode(type, payload))

            assertThat(decoded.sequence).isEqualTo(sequence.toLong())
            assertThat(decoded.messageType).isEqualTo(type)
            assertThat(decoded.plaintext).isEqualTo(payload)
        }
    }

    @Test
    fun `random single bit ciphertext corruption never authenticates`() {
        val random = Random(0x53454355)

        repeat(128) {
            val (sender, receiver) = codecs()
            val payload = random.nextBytes(random.nextInt(1, 4 * 1024))
            val frame = sender.encode(MessageType.MESSAGE_TYPE_DATA_CHUNK, payload)
            val byteIndex = random.nextInt(AesGcmFrameCodec.HEADER_BYTES, frame.size)
            val bit = 1 shl random.nextInt(8)
            frame[byteIndex] = (frame[byteIndex].toInt() xor bit).toByte()

            assertThrows(FrameSecurityException::class.java) {
                receiver.decode(frame)
            }
        }
    }

    @Test
    fun `random resume bitmaps request exactly the missing chunk complement`() {
        val random = Random(0x52455355)

        repeat(256) {
            val chunkCount = random.nextInt(1, 257)
            val received = BitSet(chunkCount)
            repeat(chunkCount) { index ->
                if (random.nextBoolean()) received.set(index)
            }
            val entry = FileEntry.newBuilder()
                .setRelativePath("images/property.jpg")
                .setSize(chunkCount.toLong() * ChunkTransferTracker.CHUNK_SIZE_BYTES)
                .setSha256(ByteString.copyFrom(ByteArray(32) { 7 }))
                .setChunkSize(ChunkTransferTracker.CHUNK_SIZE_BYTES)
                .setChunkCount(chunkCount)
                .build()
            val snapshot = ChunkResumeState.newBuilder()
                .setSessionId("property-session")
                .setRelativePath(entry.relativePath)
                .setChunkCount(chunkCount)
                .setReceivedBitmap(ByteString.copyFrom(received.toByteArray()))
                .setFileSha256(entry.sha256)
                .build()

            val missing = ChunkTransferTracker.restore(
                sessionId = "property-session",
                entry = entry,
                snapshot = snapshot,
            ).missingRequest().missingIndexesList
            val expected = (0 until chunkCount).filterNot(received::get)

            assertThat(missing).containsExactlyElementsIn(expected).inOrder()
        }
    }

    private fun codecs(): Pair<AesGcmFrameCodec, AesGcmFrameCodec> {
        val initiatorKey = ByteArray(32) { it.toByte() }
        val responderKey = ByteArray(32) { (it + 32).toByte() }
        val initiatorPrefix = byteArrayOf(1, 2, 3, 4)
        val responderPrefix = byteArrayOf(5, 6, 7, 8)
        return AesGcmFrameCodec(
            sendKey = initiatorKey,
            receiveKey = responderKey,
            sendNoncePrefix = initiatorPrefix,
            receiveNoncePrefix = responderPrefix,
        ) to AesGcmFrameCodec(
            sendKey = responderKey,
            receiveKey = initiatorKey,
            sendNoncePrefix = responderPrefix,
            receiveNoncePrefix = initiatorPrefix,
        )
    }

    companion object {
        private val MESSAGE_TYPES = listOf(
            MessageType.MESSAGE_TYPE_HELLO,
            MessageType.MESSAGE_TYPE_IDENTITY_EXCHANGE,
            MessageType.MESSAGE_TYPE_TRANSFER_OFFER,
            MessageType.MESSAGE_TYPE_MANIFEST,
            MessageType.MESSAGE_TYPE_DATA_CHUNK,
            MessageType.MESSAGE_TYPE_COMMIT_DECISION,
        )
    }
}
