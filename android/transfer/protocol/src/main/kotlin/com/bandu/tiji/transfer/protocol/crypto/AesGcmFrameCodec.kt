package com.bandu.tiji.transfer.protocol.crypto

import com.bandu.tiji.transfer.protocol.ProtocolConstants
import com.bandu.tiji.transfer.protocol.proto.MessageType
import com.bandu.tiji.transfer.protocol.proto.ProtocolErrorCode
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.GeneralSecurityException
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class DecodedFrame(
    val messageType: MessageType,
    val sequence: Long,
    val plaintext: ByteArray,
)

class FrameSecurityException(
    val code: ProtocolErrorCode,
    cause: Throwable? = null,
) : SecurityException(code.name, cause)

class AesGcmFrameCodec(
    sendKey: ByteArray,
    receiveKey: ByteArray,
    sendNoncePrefix: ByteArray,
    receiveNoncePrefix: ByteArray,
) : AutoCloseable {
    private val sendKey = sendKey.copyAndRequireSize(KEY_BYTES, "send key")
    private val receiveKey = receiveKey.copyAndRequireSize(KEY_BYTES, "receive key")
    private val sendNoncePrefix =
        sendNoncePrefix.copyAndRequireSize(NONCE_PREFIX_BYTES, "send nonce prefix")
    private val receiveNoncePrefix =
        receiveNoncePrefix.copyAndRequireSize(NONCE_PREFIX_BYTES, "receive nonce prefix")
    private var nextSendSequence = 0L
    private var nextReceiveSequence = 0L
    private var closed = false

    fun encode(
        messageType: MessageType,
        plaintext: ByteArray,
    ): ByteArray {
        requireOpen()
        if (messageType == MessageType.MESSAGE_TYPE_UNSPECIFIED) {
            throw FrameSecurityException(ProtocolErrorCode.PROTOCOL_ERROR_CODE_INVALID_MESSAGE)
        }
        if (plaintext.size > MAX_CIPHERTEXT_BYTES) {
            throw FrameSecurityException(ProtocolErrorCode.PROTOCOL_ERROR_CODE_FRAME_TOO_LARGE)
        }
        if (nextSendSequence < 0) {
            throw FrameSecurityException(ProtocolErrorCode.PROTOCOL_ERROR_CODE_REPLAY_DETECTED)
        }

        val sequence = nextSendSequence
        val header = encodeHeader(messageType, sequence, plaintext.size)
        val encrypted = cipher(
            mode = Cipher.ENCRYPT_MODE,
            key = sendKey,
            nonce = nonce(sendNoncePrefix, sequence),
            aad = header,
        ).doFinal(plaintext)
        nextSendSequence += 1
        return header + encrypted
    }

    fun decode(frame: ByteArray): DecodedFrame {
        requireOpen()
        if (frame.size < HEADER_BYTES + GCM_TAG_BYTES) {
            throw FrameSecurityException(ProtocolErrorCode.PROTOCOL_ERROR_CODE_INVALID_MESSAGE)
        }

        val header = frame.copyOfRange(0, HEADER_BYTES)
        val parsed = parseHeader(header)
        if (parsed.ciphertextLength > MAX_CIPHERTEXT_BYTES) {
            throw FrameSecurityException(ProtocolErrorCode.PROTOCOL_ERROR_CODE_FRAME_TOO_LARGE)
        }
        val expectedFrameSize = HEADER_BYTES.toLong() + parsed.ciphertextLength + GCM_TAG_BYTES
        if (expectedFrameSize != frame.size.toLong()) {
            throw FrameSecurityException(ProtocolErrorCode.PROTOCOL_ERROR_CODE_INVALID_MESSAGE)
        }
        if (parsed.sequence < nextReceiveSequence) {
            throw FrameSecurityException(ProtocolErrorCode.PROTOCOL_ERROR_CODE_REPLAY_DETECTED)
        }
        if (parsed.sequence > nextReceiveSequence) {
            throw FrameSecurityException(ProtocolErrorCode.PROTOCOL_ERROR_CODE_OUT_OF_ORDER)
        }

        val plaintext = try {
            cipher(
                mode = Cipher.DECRYPT_MODE,
                key = receiveKey,
                nonce = nonce(receiveNoncePrefix, parsed.sequence),
                aad = header,
            ).doFinal(frame, HEADER_BYTES, frame.size - HEADER_BYTES)
        } catch (error: AEADBadTagException) {
            throw FrameSecurityException(
                ProtocolErrorCode.PROTOCOL_ERROR_CODE_INTEGRITY_FAILED,
                error,
            )
        } catch (error: GeneralSecurityException) {
            throw FrameSecurityException(
                ProtocolErrorCode.PROTOCOL_ERROR_CODE_INVALID_MESSAGE,
                error,
            )
        }
        nextReceiveSequence += 1
        return DecodedFrame(parsed.messageType, parsed.sequence, plaintext)
    }

    override fun close() {
        sendKey.fill(0)
        receiveKey.fill(0)
        sendNoncePrefix.fill(0)
        receiveNoncePrefix.fill(0)
        closed = true
    }

    private fun requireOpen() {
        check(!closed) { "Frame codec is closed" }
    }

    private fun parseHeader(header: ByteArray): ParsedHeader {
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.BIG_ENDIAN)
        val magic = ByteArray(MAGIC.size).also(buffer::get)
        if (!magic.contentEquals(MAGIC)) {
            throw FrameSecurityException(ProtocolErrorCode.PROTOCOL_ERROR_CODE_INVALID_MESSAGE)
        }
        val version = buffer.short.toInt() and 0xffff
        if (version != ProtocolConstants.VERSION) {
            throw FrameSecurityException(ProtocolErrorCode.PROTOCOL_ERROR_CODE_UNSUPPORTED_VERSION)
        }
        val messageType = MessageType.forNumber(buffer.short.toInt() and 0xffff)
        if (messageType == null || messageType == MessageType.MESSAGE_TYPE_UNSPECIFIED) {
            throw FrameSecurityException(ProtocolErrorCode.PROTOCOL_ERROR_CODE_INVALID_MESSAGE)
        }
        val sequence = buffer.long
        val ciphertextLength = buffer.int
        if (sequence < 0 || ciphertextLength < 0) {
            throw FrameSecurityException(ProtocolErrorCode.PROTOCOL_ERROR_CODE_INVALID_MESSAGE)
        }
        return ParsedHeader(messageType, sequence, ciphertextLength)
    }

    private fun encodeHeader(
        messageType: MessageType,
        sequence: Long,
        ciphertextLength: Int,
    ): ByteArray = ByteBuffer.allocate(HEADER_BYTES)
        .order(ByteOrder.BIG_ENDIAN)
        .put(MAGIC)
        .putShort(ProtocolConstants.VERSION.toShort())
        .putShort(messageType.number.toShort())
        .putLong(sequence)
        .putInt(ciphertextLength)
        .array()

    private fun nonce(prefix: ByteArray, sequence: Long): ByteArray =
        ByteBuffer.allocate(GCM_NONCE_BYTES)
            .order(ByteOrder.BIG_ENDIAN)
            .put(prefix)
            .putLong(sequence)
            .array()

    private fun cipher(
        mode: Int,
        key: ByteArray,
        nonce: ByteArray,
        aad: ByteArray,
    ): Cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
        init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, nonce))
        updateAAD(aad)
    }

    private data class ParsedHeader(
        val messageType: MessageType,
        val sequence: Long,
        val ciphertextLength: Int,
    )

    companion object {
        const val MAX_CIPHERTEXT_BYTES = 2 * 1024 * 1024
        const val HEADER_BYTES = 20
        const val GCM_TAG_BYTES = 16
        private const val KEY_BYTES = 32
        private const val NONCE_PREFIX_BYTES = 4
        private const val GCM_NONCE_BYTES = 12
        private const val GCM_TAG_BITS = GCM_TAG_BYTES * 8
        private val MAGIC = byteArrayOf('B'.code.toByte(), 'T'.code.toByte(), 'J'.code.toByte(), '1'.code.toByte())
    }
}

private fun ByteArray.copyAndRequireSize(expectedSize: Int, label: String): ByteArray =
    copyOf().also {
        require(it.size == expectedSize) { "$label must contain $expectedSize bytes" }
    }
