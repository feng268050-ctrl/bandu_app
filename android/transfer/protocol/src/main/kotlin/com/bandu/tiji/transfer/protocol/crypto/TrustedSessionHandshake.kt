package com.bandu.tiji.transfer.protocol.crypto

import com.bandu.tiji.transfer.protocol.identity.IdentityProof
import com.bandu.tiji.transfer.protocol.proto.ProtocolErrorCode
import com.bandu.tiji.transfer.protocol.proto.TrustedSessionHello
import com.bandu.tiji.transfer.protocol.proto.TrustedSessionProof
import com.google.protobuf.ByteString
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.GeneralSecurityException
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import javax.crypto.KeyAgreement

enum class SessionRole {
    INITIATOR,
    RESPONDER,
}

data class DirectionalSessionKeys(
    val sendKey: ByteArray,
    val receiveKey: ByteArray,
    val sendNoncePrefix: ByteArray,
    val receiveNoncePrefix: ByteArray,
)

class TrustedSessionException(
    val code: ProtocolErrorCode,
    cause: Throwable? = null,
) : SecurityException(code.name, cause)

class TrustedSessionParticipant private constructor(
    private val role: SessionRole,
    private val identitySigningPrivateKey: PrivateKey,
    private val ephemeralPrivateKey: PrivateKey,
    val hello: TrustedSessionHello,
) {
    fun createProof(peerHello: TrustedSessionHello): TrustedSessionProof =
        mapSecurityFailure {
            val signature = Signature.getInstance(SIGNATURE_ALGORITHM).run {
                initSign(identitySigningPrivateKey)
                update(transcript(hello, peerHello))
                sign()
            }
            TrustedSessionProof.newBuilder()
                .setTranscriptSignature(ByteString.copyFrom(signature))
                .build()
        }

    fun establish(
        peerHello: TrustedSessionHello,
        peerProof: TrustedSessionProof,
        trustedPeerDeviceId: String,
        trustedPeerSigningPublicKey: ByteArray,
    ): DirectionalSessionKeys = mapSecurityFailure {
        if (peerHello.deviceId != trustedPeerDeviceId ||
            peerHello.deviceId == hello.deviceId ||
            peerHello.nonce.size() != NONCE_BYTES ||
            peerProof.transcriptSignature.isEmpty
        ) {
            throw TrustedSessionException(ProtocolErrorCode.PROTOCOL_ERROR_CODE_IDENTITY_MISMATCH)
        }
        val handshakeTranscript = transcript(hello, peerHello)
        val trustedPublicKey = IdentityProof.decodeP256PublicKey(trustedPeerSigningPublicKey)
        val validSignature = Signature.getInstance(SIGNATURE_ALGORITHM).run {
            initVerify(trustedPublicKey)
            update(handshakeTranscript)
            verify(peerProof.transcriptSignature.toByteArray())
        }
        if (!validSignature) {
            throw TrustedSessionException(ProtocolErrorCode.PROTOCOL_ERROR_CODE_AUTHENTICATION_FAILED)
        }

        val peerEphemeralKey = IdentityProof.decodeP256PublicKey(
            peerHello.ephemeralPublicKey.toByteArray(),
        )
        val sharedSecret = KeyAgreement.getInstance("ECDH").run {
            init(ephemeralPrivateKey)
            doPhase(peerEphemeralKey, true)
            generateSecret()
        }
        val keyMaterial = try {
            HkdfSha256.derive(
                inputKeyMaterial = sharedSecret,
                salt = MessageDigest.getInstance("SHA-256").digest(handshakeTranscript),
                info = KEY_DERIVATION_INFO,
                length = KEY_MATERIAL_BYTES,
            )
        } finally {
            sharedSecret.fill(0)
        }
        try {
            val initiatorToResponderKey = keyMaterial.copyOfRange(0, KEY_BYTES)
            val responderToInitiatorKey = keyMaterial.copyOfRange(KEY_BYTES, KEY_BYTES * 2)
            val initiatorNoncePrefix = keyMaterial.copyOfRange(KEY_BYTES * 2, KEY_BYTES * 2 + NONCE_PREFIX_BYTES)
            val responderNoncePrefix = keyMaterial.copyOfRange(
                KEY_BYTES * 2 + NONCE_PREFIX_BYTES,
                KEY_MATERIAL_BYTES,
            )
            if (role == SessionRole.INITIATOR) {
                DirectionalSessionKeys(
                    sendKey = initiatorToResponderKey,
                    receiveKey = responderToInitiatorKey,
                    sendNoncePrefix = initiatorNoncePrefix,
                    receiveNoncePrefix = responderNoncePrefix,
                )
            } else {
                DirectionalSessionKeys(
                    sendKey = responderToInitiatorKey,
                    receiveKey = initiatorToResponderKey,
                    sendNoncePrefix = responderNoncePrefix,
                    receiveNoncePrefix = initiatorNoncePrefix,
                )
            }
        } finally {
            keyMaterial.fill(0)
        }
    }

    private fun transcript(
        localHello: TrustedSessionHello,
        peerHello: TrustedSessionHello,
    ): ByteArray {
        val (initiatorHello, responderHello) = if (role == SessionRole.INITIATOR) {
            localHello to peerHello
        } else {
            peerHello to localHello
        }
        return ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { output ->
                output.writeLengthPrefixed(TRANSCRIPT_DOMAIN)
                output.writeHello(initiatorHello)
                output.writeHello(responderHello)
            }
            bytes.toByteArray()
        }
    }

    companion object {
        private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
        private const val NONCE_BYTES = 32
        private const val KEY_BYTES = 32
        private const val NONCE_PREFIX_BYTES = 4
        private const val KEY_MATERIAL_BYTES = KEY_BYTES * 2 + NONCE_PREFIX_BYTES * 2
        private val TRANSCRIPT_DOMAIN = "bandu-tiji-trusted-transcript-v1".encodeToByteArray()
        private val KEY_DERIVATION_INFO = "bandu-tiji-trusted-session-keys-v1".encodeToByteArray()

        fun create(
            role: SessionRole,
            deviceId: String,
            identitySigningPrivateKey: PrivateKey,
            secureRandom: SecureRandom = SecureRandom(),
        ): TrustedSessionParticipant {
            require(deviceId.isNotBlank()) { "Device ID must not be blank" }
            val ephemeralKeys = KeyPairGenerator.getInstance("EC").run {
                initialize(ECGenParameterSpec("secp256r1"), secureRandom)
                generateKeyPair()
            }
            val nonce = ByteArray(NONCE_BYTES).also(secureRandom::nextBytes)
            val hello = TrustedSessionHello.newBuilder()
                .setDeviceId(deviceId)
                .setNonce(ByteString.copyFrom(nonce))
                .setEphemeralPublicKey(ByteString.copyFrom(ephemeralKeys.public.encoded))
                .build()
            return TrustedSessionParticipant(
                role = role,
                identitySigningPrivateKey = identitySigningPrivateKey,
                ephemeralPrivateKey = ephemeralKeys.private,
                hello = hello,
            )
        }
    }
}

private fun DataOutputStream.writeHello(hello: TrustedSessionHello) {
    writeLengthPrefixed(hello.deviceId.encodeToByteArray())
    writeLengthPrefixed(hello.nonce.toByteArray())
    writeLengthPrefixed(hello.ephemeralPublicKey.toByteArray())
}

private fun DataOutputStream.writeLengthPrefixed(value: ByteArray) {
    writeInt(value.size)
    write(value)
}

private inline fun <T> mapSecurityFailure(block: () -> T): T = try {
    block()
} catch (error: TrustedSessionException) {
    throw error
} catch (error: GeneralSecurityException) {
    throw TrustedSessionException(
        ProtocolErrorCode.PROTOCOL_ERROR_CODE_AUTHENTICATION_FAILED,
        error,
    )
}
