package com.bandu.tiji.transfer.protocol.crypto

import com.bandu.tiji.transfer.protocol.proto.MessageType
import com.bandu.tiji.transfer.protocol.proto.ProtocolErrorCode
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AesGcmFrameCodecTest {
    private val initiatorKey = ByteArray(32) { it.toByte() }
    private val responderKey = ByteArray(32) { (it + 32).toByte() }
    private val initiatorPrefix = byteArrayOf(1, 2, 3, 4)
    private val responderPrefix = byteArrayOf(5, 6, 7, 8)

    @Test
    fun `frame round trips with authenticated header and strict sequence`() {
        val (sender, receiver) = codecs()
        val frame = sender.encode(
            MessageType.MESSAGE_TYPE_TRANSFER_OFFER,
            "payload".encodeToByteArray(),
        )

        val decoded = receiver.decode(frame)

        assertThat(frame.copyOfRange(0, 4)).isEqualTo("BTJ1".encodeToByteArray())
        assertThat(decoded.messageType).isEqualTo(MessageType.MESSAGE_TYPE_TRANSFER_OFFER)
        assertThat(decoded.sequence).isEqualTo(0)
        assertThat(decoded.plaintext.decodeToString()).isEqualTo("payload")
    }

    @Test
    fun `ciphertext and aad tampering fail authentication`() {
        val (sender, receiver) = codecs()
        val ciphertextTampered = sender.encode(
            MessageType.MESSAGE_TYPE_HELLO,
            "payload".encodeToByteArray(),
        )
        ciphertextTampered[AesGcmFrameCodec.HEADER_BYTES] =
            (ciphertextTampered[AesGcmFrameCodec.HEADER_BYTES].toInt() xor 1).toByte()

        assertCode(ProtocolErrorCode.PROTOCOL_ERROR_CODE_INTEGRITY_FAILED) {
            receiver.decode(ciphertextTampered)
        }

        val (aadSender, aadReceiver) = codecs()
        val aadTampered = aadSender.encode(
            MessageType.MESSAGE_TYPE_HELLO,
            "payload".encodeToByteArray(),
        )
        aadTampered[7] = MessageType.MESSAGE_TYPE_PAIRING_START.number.toByte()
        assertCode(ProtocolErrorCode.PROTOCOL_ERROR_CODE_INTEGRITY_FAILED) {
            aadReceiver.decode(aadTampered)
        }
    }

    @Test
    fun `replayed and out of order frames are rejected`() {
        val (sender, receiver) = codecs()
        val first = sender.encode(MessageType.MESSAGE_TYPE_HELLO, byteArrayOf(1))
        val second = sender.encode(MessageType.MESSAGE_TYPE_HELLO, byteArrayOf(2))

        receiver.decode(first)
        assertCode(ProtocolErrorCode.PROTOCOL_ERROR_CODE_REPLAY_DETECTED) {
            receiver.decode(first)
        }

        val (_, freshReceiver) = codecs()
        assertCode(ProtocolErrorCode.PROTOCOL_ERROR_CODE_OUT_OF_ORDER) {
            freshReceiver.decode(second)
        }
    }

    @Test
    fun `sequence produces unique nonce inputs and duplicate nonce frame is rejected`() {
        val (sender, receiver) = codecs()
        val first = sender.encode(MessageType.MESSAGE_TYPE_HELLO, byteArrayOf(7))
        val second = sender.encode(MessageType.MESSAGE_TYPE_HELLO, byteArrayOf(7))

        assertThat(sequence(first)).isEqualTo(0)
        assertThat(sequence(second)).isEqualTo(1)
        assertThat(first.copyOfRange(AesGcmFrameCodec.HEADER_BYTES, first.size))
            .isNotEqualTo(second.copyOfRange(AesGcmFrameCodec.HEADER_BYTES, second.size))

        receiver.decode(first)
        assertCode(ProtocolErrorCode.PROTOCOL_ERROR_CODE_REPLAY_DETECTED) {
            receiver.decode(first)
        }
    }

    @Test
    fun `complete frame is limited to two mebibytes`() {
        val (sender, receiver) = codecs()
        val largest = sender.encode(
            MessageType.MESSAGE_TYPE_DATA_CHUNK,
            ByteArray(AesGcmFrameCodec.MAX_CIPHERTEXT_BYTES),
        )

        assertThat(largest).hasLength(AesGcmFrameCodec.MAX_FRAME_BYTES)
        assertThat(receiver.decode(largest).plaintext)
            .hasLength(AesGcmFrameCodec.MAX_CIPHERTEXT_BYTES)

        assertCode(ProtocolErrorCode.PROTOCOL_ERROR_CODE_FRAME_TOO_LARGE) {
            sender.encode(
                MessageType.MESSAGE_TYPE_DATA_CHUNK,
                ByteArray(AesGcmFrameCodec.MAX_CIPHERTEXT_BYTES + 1),
            )
        }
    }

    private fun codecs(): Pair<AesGcmFrameCodec, AesGcmFrameCodec> =
        AesGcmFrameCodec(
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

    private fun sequence(frame: ByteArray): Long =
        ByteBuffer.wrap(frame, 8, 8).order(ByteOrder.BIG_ENDIAN).long

    private fun assertCode(
        expected: ProtocolErrorCode,
        block: () -> Unit,
    ) {
        val error = assertThrows(FrameSecurityException::class.java, block)
        assertThat(error.code).isEqualTo(expected)
    }
}
