package com.bandu.tiji.transfer.protocol.identity

import com.bandu.tiji.transfer.protocol.proto.IdentityExchange
import com.bandu.tiji.transfer.protocol.proto.ProtocolErrorCode
import com.google.protobuf.ByteString
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.AlgorithmParameters
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECFieldFp
import java.security.spec.ECParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.security.MessageDigest
import java.security.spec.ECGenParameterSpec

data class VerifiedDeviceIdentity(
    val deviceId: String,
    val displayName: String,
    val signingPublicKey: ByteArray,
    val fingerprint: String,
)

class IdentityVerificationException(
    val code: ProtocolErrorCode = ProtocolErrorCode.PROTOCOL_ERROR_CODE_IDENTITY_MISMATCH,
    cause: Throwable? = null,
) : SecurityException(code.name, cause)

object IdentityProof {
    private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
    private val DOMAIN = "bandu-tiji-identity-proof-v1".encodeToByteArray()
    private val P256_PARAMETERS: ECParameterSpec =
        AlgorithmParameters.getInstance("EC").run {
            init(ECGenParameterSpec("secp256r1"))
            getParameterSpec(ECParameterSpec::class.java)
        }

    fun createExchange(
        deviceId: String,
        displayName: String,
        signingPublicKey: PublicKey,
        signingPrivateKey: PrivateKey,
        authenticatedChannelContext: ByteArray,
    ): IdentityExchange {
        require(deviceId.isNotBlank()) { "Device ID must not be blank" }
        require(authenticatedChannelContext.isNotEmpty()) {
            "Authenticated channel context must not be empty"
        }
        val encodedPublicKey = signingPublicKey.encoded.copyOf()
        validateP256PublicKey(signingPublicKey)
        val signature = Signature.getInstance(SIGNATURE_ALGORITHM).run {
            initSign(signingPrivateKey)
            update(proofPayload(deviceId, displayName, encodedPublicKey, authenticatedChannelContext))
            sign()
        }
        return IdentityExchange.newBuilder()
            .setDeviceId(deviceId)
            .setDisplayName(displayName)
            .setSigningPublicKey(ByteString.copyFrom(encodedPublicKey))
            .setProofSignature(ByteString.copyFrom(signature))
            .build()
    }

    fun verify(
        exchange: IdentityExchange,
        authenticatedChannelContext: ByteArray,
        trustedSigningPublicKey: ByteArray? = null,
    ): VerifiedDeviceIdentity = try {
        if (exchange.deviceId.isBlank() || authenticatedChannelContext.isEmpty()) {
            throw IdentityVerificationException()
        }
        val encodedPublicKey = exchange.signingPublicKey.toByteArray()
        if (trustedSigningPublicKey != null &&
            !MessageDigest.isEqual(trustedSigningPublicKey, encodedPublicKey)
        ) {
            throw IdentityVerificationException()
        }
        val publicKey = decodeP256PublicKey(encodedPublicKey)
        val valid = Signature.getInstance(SIGNATURE_ALGORITHM).run {
            initVerify(publicKey)
            update(
                proofPayload(
                    exchange.deviceId,
                    exchange.displayName,
                    encodedPublicKey,
                    authenticatedChannelContext,
                ),
            )
            verify(exchange.proofSignature.toByteArray())
        }
        if (!valid) throw IdentityVerificationException()
        VerifiedDeviceIdentity(
            deviceId = exchange.deviceId,
            displayName = exchange.displayName,
            signingPublicKey = encodedPublicKey,
            fingerprint = fingerprint(encodedPublicKey),
        )
    } catch (error: IdentityVerificationException) {
        throw error
    } catch (error: GeneralSecurityException) {
        throw IdentityVerificationException(cause = error)
    }

    fun fingerprint(signingPublicKey: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(signingPublicKey)
            .joinToString("") { byte -> "%02x".format(byte) }

    fun decodeP256PublicKey(encoded: ByteArray): PublicKey =
        KeyFactory.getInstance("EC")
            .generatePublic(X509EncodedKeySpec(encoded))
            .also(::validateP256PublicKey)

    private fun validateP256PublicKey(publicKey: PublicKey) {
        val ecKey = publicKey as? ECPublicKey ?: throw IdentityVerificationException()
        val actual = ecKey.params
        val expected = P256_PARAMETERS
        val actualField = actual.curve.field as? ECFieldFp
        val expectedField = expected.curve.field as ECFieldFp
        if (actualField?.p != expectedField.p ||
            actual.curve.a != expected.curve.a ||
            actual.curve.b != expected.curve.b ||
            actual.generator != expected.generator ||
            actual.order != expected.order ||
            actual.cofactor != expected.cofactor
        ) {
            throw IdentityVerificationException()
        }
    }

    private fun proofPayload(
        deviceId: String,
        displayName: String,
        encodedPublicKey: ByteArray,
        authenticatedChannelContext: ByteArray,
    ): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { output ->
            output.writeLengthPrefixed(DOMAIN)
            output.writeLengthPrefixed(deviceId.encodeToByteArray())
            output.writeLengthPrefixed(displayName.encodeToByteArray())
            output.writeLengthPrefixed(encodedPublicKey)
            output.writeLengthPrefixed(
                MessageDigest.getInstance("SHA-256").digest(authenticatedChannelContext),
            )
        }
        bytes.toByteArray()
    }

    private fun DataOutputStream.writeLengthPrefixed(value: ByteArray) {
        writeInt(value.size)
        write(value)
    }
}
