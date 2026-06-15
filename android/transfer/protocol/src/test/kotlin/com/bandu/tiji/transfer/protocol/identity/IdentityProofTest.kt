package com.bandu.tiji.transfer.protocol.identity

import com.bandu.tiji.transfer.protocol.proto.ProtocolErrorCode
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec

class IdentityProofTest {
    private val channelContext = "authenticated-srp-transcript".encodeToByteArray()

    @Test
    fun `valid identity proves signing key possession and exposes stable fingerprint`() {
        val identity = generateIdentity()
        val exchange = IdentityProof.createExchange(
            deviceId = "device-a",
            displayName = "Phone A",
            signingPublicKey = identity.public,
            signingPrivateKey = identity.private,
            authenticatedChannelContext = channelContext,
        )

        val verified = IdentityProof.verify(
            exchange = exchange,
            authenticatedChannelContext = channelContext,
            trustedSigningPublicKey = identity.public.encoded,
        )

        assertThat(verified.deviceId).isEqualTo("device-a")
        assertThat(verified.displayName).isEqualTo("Phone A")
        assertThat(verified.signingPublicKey).isEqualTo(identity.public.encoded)
        assertThat(verified.fingerprint).hasLength(64)
        assertThat(verified.fingerprint)
            .isEqualTo(IdentityProof.fingerprint(identity.public.encoded))
    }

    @Test
    fun `identity replacement with a valid attacker signature is rejected`() {
        val trustedIdentity = generateIdentity()
        val replacementIdentity = generateIdentity()
        val replacement = IdentityProof.createExchange(
            deviceId = "device-a",
            displayName = "Phone A",
            signingPublicKey = replacementIdentity.public,
            signingPrivateKey = replacementIdentity.private,
            authenticatedChannelContext = channelContext,
        )

        val error = assertThrows(IdentityVerificationException::class.java) {
            IdentityProof.verify(
                exchange = replacement,
                authenticatedChannelContext = channelContext,
                trustedSigningPublicKey = trustedIdentity.public.encoded,
            )
        }

        assertThat(error.code)
            .isEqualTo(ProtocolErrorCode.PROTOCOL_ERROR_CODE_IDENTITY_MISMATCH)
    }

    @Test
    fun `tampered identity fields or channel context invalidate proof`() {
        val identity = generateIdentity()
        val exchange = IdentityProof.createExchange(
            deviceId = "device-a",
            displayName = "Phone A",
            signingPublicKey = identity.public,
            signingPrivateKey = identity.private,
            authenticatedChannelContext = channelContext,
        )

        assertThrows(IdentityVerificationException::class.java) {
            IdentityProof.verify(
                exchange.toBuilder().setDisplayName("Phone B").build(),
                channelContext,
            )
        }
        assertThrows(IdentityVerificationException::class.java) {
            IdentityProof.verify(exchange, "different-transcript".encodeToByteArray())
        }
    }

    @Test
    fun `identity proof rejects signing keys outside P-256`() {
        val p384 = generateIdentity("secp384r1")
        val p256 = generateIdentity()

        assertThrows(IdentityVerificationException::class.java) {
            IdentityProof.createExchange(
                deviceId = "device-a",
                displayName = "Phone A",
                signingPublicKey = p384.public,
                signingPrivateKey = p256.private,
                authenticatedChannelContext = channelContext,
            )
        }
    }

    private fun generateIdentity(curve: String = "secp256r1"): KeyPair =
        KeyPairGenerator.getInstance("EC").run {
            initialize(ECGenParameterSpec(curve))
            generateKeyPair()
        }
}
