package com.bandu.tiji.transfer.protocol

import com.bandu.tiji.transfer.protocol.proto.ProtocolEnvelope
import com.bandu.tiji.transfer.protocol.proto.ProtocolErrorCode

object ProtocolConstants {
    const val VERSION: Int = 1
}

class ProtocolViolationException(
    val code: ProtocolErrorCode,
    message: String,
) : IllegalArgumentException(message)

object ProtocolEnvelopeCodec {
    fun encode(envelope: ProtocolEnvelope): ByteArray {
        requireSupportedVersion(envelope.protocolVersion)
        return envelope.toByteArray()
    }

    fun decode(bytes: ByteArray): ProtocolEnvelope =
        ProtocolEnvelope.parseFrom(bytes).also { envelope ->
            requireSupportedVersion(envelope.protocolVersion)
        }

    private fun requireSupportedVersion(version: Int) {
        if (version != ProtocolConstants.VERSION) {
            throw ProtocolViolationException(
                code = ProtocolErrorCode.PROTOCOL_ERROR_CODE_UNSUPPORTED_VERSION,
                message = "Unsupported protocol version: $version",
            )
        }
    }
}
