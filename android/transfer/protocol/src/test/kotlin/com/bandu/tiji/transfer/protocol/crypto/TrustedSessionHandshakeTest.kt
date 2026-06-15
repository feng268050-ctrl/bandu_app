package com.bandu.tiji.transfer.protocol.crypto

import com.bandu.tiji.transfer.protocol.proto.ProtocolErrorCode
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec

class TrustedSessionHandshakeTest {
    @Test
    fun `both peers derive mirrored directional keys and nonce prefixes`() {
        val initiatorIdentity = generateIdentity()
        val responderIdentity = generateIdentity()
        val initiator = TrustedSessionParticipant.create(
            SessionRole.INITIATOR,
            "device-a",
            initiatorIdentity.private,
        )
        val responder = TrustedSessionParticipant.create(
            SessionRole.RESPONDER,
            "device-b",
            responderIdentity.private,
        )

        val initiatorKeys = initiator.establish(
            peerHello = responder.hello,
            peerProof = responder.createProof(initiator.hello),
            trustedPeerDeviceId = "device-b",
            trustedPeerSigningPublicKey = responderIdentity.public.encoded,
        )
        val responderKeys = responder.establish(
            peerHello = initiator.hello,
            peerProof = initiator.createProof(responder.hello),
            trustedPeerDeviceId = "device-a",
            trustedPeerSigningPublicKey = initiatorIdentity.public.encoded,
        )

        assertThat(initiatorKeys.sendKey).isEqualTo(responderKeys.receiveKey)
        assertThat(initiatorKeys.receiveKey).isEqualTo(responderKeys.sendKey)
        assertThat(initiatorKeys.sendNoncePrefix).isEqualTo(responderKeys.receiveNoncePrefix)
        assertThat(initiatorKeys.receiveNoncePrefix).isEqualTo(responderKeys.sendNoncePrefix)
        assertThat(initiatorKeys.sendKey).isNotEqualTo(initiatorKeys.receiveKey)
        assertThat(initiatorKeys.sendNoncePrefix)
            .isNotEqualTo(initiatorKeys.receiveNoncePrefix)
    }

    @Test
    fun `tampered transcript is rejected`() {
        val initiatorIdentity = generateIdentity()
        val responderIdentity = generateIdentity()
        val initiator = TrustedSessionParticipant.create(
            SessionRole.INITIATOR,
            "device-a",
            initiatorIdentity.private,
        )
        val responder = TrustedSessionParticipant.create(
            SessionRole.RESPONDER,
            "device-b",
            responderIdentity.private,
        )
        val proof = responder.createProof(initiator.hello)
        val tamperedHello = responder.hello.toBuilder()
            .setDeviceId("device-c")
            .build()

        val error = assertThrows(TrustedSessionException::class.java) {
            initiator.establish(
                peerHello = tamperedHello,
                peerProof = proof,
                trustedPeerDeviceId = "device-c",
                trustedPeerSigningPublicKey = responderIdentity.public.encoded,
            )
        }

        assertThat(error.code)
            .isEqualTo(ProtocolErrorCode.PROTOCOL_ERROR_CODE_AUTHENTICATION_FAILED)
    }

    @Test
    fun `replacement signing key cannot authenticate trusted peer`() {
        val initiatorIdentity = generateIdentity()
        val responderIdentity = generateIdentity()
        val replacementIdentity = generateIdentity()
        val initiator = TrustedSessionParticipant.create(
            SessionRole.INITIATOR,
            "device-a",
            initiatorIdentity.private,
        )
        val replacement = TrustedSessionParticipant.create(
            SessionRole.RESPONDER,
            "device-b",
            replacementIdentity.private,
        )

        val error = assertThrows(TrustedSessionException::class.java) {
            initiator.establish(
                peerHello = replacement.hello,
                peerProof = replacement.createProof(initiator.hello),
                trustedPeerDeviceId = "device-b",
                trustedPeerSigningPublicKey = responderIdentity.public.encoded,
            )
        }

        assertThat(error.code)
            .isEqualTo(ProtocolErrorCode.PROTOCOL_ERROR_CODE_AUTHENTICATION_FAILED)
    }

    private fun generateIdentity(): KeyPair =
        KeyPairGenerator.getInstance("EC").run {
            initialize(ECGenParameterSpec("secp256r1"))
            generateKeyPair()
        }
}
