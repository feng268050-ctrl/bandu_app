package com.bandu.tiji.transfer.protocol

import com.bandu.tiji.transfer.protocol.proto.MessageType
import com.bandu.tiji.transfer.protocol.proto.ProtocolEnvelope
import com.bandu.tiji.transfer.protocol.proto.ProtocolErrorCode
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class ProtocolEnvelopeCodecTest {
    @Test
    fun `supported protocol version round trips`() {
        val envelope = ProtocolEnvelope.newBuilder()
            .setProtocolVersion(ProtocolConstants.VERSION)
            .setMessageType(MessageType.MESSAGE_TYPE_HELLO)
            .build()

        assertThat(ProtocolEnvelopeCodec.decode(ProtocolEnvelopeCodec.encode(envelope)))
            .isEqualTo(envelope)
    }

    @Test
    fun `unknown protocol version is rejected`() {
        val unsupported = ProtocolEnvelope.newBuilder()
            .setProtocolVersion(ProtocolConstants.VERSION + 1)
            .setMessageType(MessageType.MESSAGE_TYPE_HELLO)
            .build()
            .toByteArray()

        val error = assertThrows(ProtocolViolationException::class.java) {
            ProtocolEnvelopeCodec.decode(unsupported)
        }

        assertThat(error.code)
            .isEqualTo(ProtocolErrorCode.PROTOCOL_ERROR_CODE_UNSUPPORTED_VERSION)
    }
}
