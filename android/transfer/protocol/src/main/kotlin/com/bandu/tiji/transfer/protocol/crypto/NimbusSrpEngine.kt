package com.bandu.tiji.transfer.protocol.crypto

import com.bandu.tiji.core.common.time.Clock
import com.bandu.tiji.core.common.time.SystemClock
import com.bandu.tiji.transfer.protocol.proto.ProtocolErrorCode
import com.nimbusds.srp6.BigIntegerUtils
import com.nimbusds.srp6.SRP6ClientSession
import com.nimbusds.srp6.SRP6CryptoParams
import com.nimbusds.srp6.SRP6Exception
import com.nimbusds.srp6.SRP6ServerSession
import com.nimbusds.srp6.SRP6VerifierGenerator
import com.nimbusds.srp6.XRoutineWithUserIdentity
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom

class NimbusSrpEngine(
    private val secureRandom: SecureRandom = SecureRandom(),
    private val clock: Clock = SystemClock(),
) : SrpEngine {
    override fun createServer(code: CharArray, identity: ByteArray): SrpServerSession =
        NimbusServerSession(code.copyOf(), identity.copyOf(), secureRandom, clock)

    override fun createClient(code: CharArray, identity: ByteArray): SrpClientSession =
        NimbusClientSession(code.copyOf(), identity.copyOf(), clock)

    internal companion object {
        const val GROUP_BITS = 2048
        const val HASH_ALGORITHM = "SHA-256"
        const val SALT_BYTES = 16
        val PARAMS: SRP6CryptoParams = requireNotNull(
            SRP6CryptoParams.getInstance(GROUP_BITS, HASH_ALGORITHM),
        )
        val RFC_X_ROUTINE = XRoutineWithUserIdentity()
    }
}

private class NimbusClientSession(
    private val code: CharArray,
    private val identity: ByteArray,
    private val clock: Clock,
) : SrpClientSession {
    private val expiresAt = clock.nowEpochMillis() + SRP_SESSION_MILLIS
    private val delegate = SRP6ClientSession().apply {
        setXRoutine(NimbusSrpEngine.RFC_X_ROUTINE)
    }

    override fun start(): SrpClientHello {
        requireNotExpired()
        delegate.step1(identity.decodeToString(), code.concatToString())
        return SrpClientHello(identity.copyOf())
    }

    override fun answer(challenge: SrpServerChallenge): SrpClientProof {
        requireNotExpired()
        return mapSrpFailures {
            val credentials = delegate.step2(
                NimbusSrpEngine.PARAMS,
                challenge.salt.toPositiveBigInteger(),
                challenge.serverPublicValue.toPositiveBigInteger(),
            )
            SrpClientProof(
                clientPublicValue = credentials.A.toUnsignedBytes(),
                clientEvidence = credentials.M1.toUnsignedBytes(),
            )
        }
    }

    override fun verify(serverEvidence: ByteArray): ByteArray {
        requireNotExpired()
        return mapSrpFailures {
            delegate.step3(serverEvidence.toPositiveBigInteger())
            delegate.sessionKey.toUnsignedBytes().deriveTemporaryKey(identity)
        }
    }

    private fun requireNotExpired() = requireSrpNotExpired(clock, expiresAt)
}

private class NimbusServerSession(
    private val code: CharArray,
    private val identity: ByteArray,
    private val secureRandom: SecureRandom,
    private val clock: Clock,
) : SrpServerSession {
    private val expiresAt = clock.nowEpochMillis() + SRP_SESSION_MILLIS
    private val delegate = SRP6ServerSession(NimbusSrpEngine.PARAMS)

    override fun challenge(hello: SrpClientHello): SrpServerChallenge {
        requireNotExpired()
        require(hello.identity.contentEquals(identity)) { "SRP identity mismatch" }
        val verifierGenerator = SRP6VerifierGenerator(NimbusSrpEngine.PARAMS).apply {
            setXRoutine(NimbusSrpEngine.RFC_X_ROUTINE)
        }
        val salt = verifierGenerator.generateRandomSalt(NimbusSrpEngine.SALT_BYTES, secureRandom)
        val verifier = verifierGenerator.generateVerifier(
            salt,
            identity,
            code.concatToString().toByteArray(StandardCharsets.UTF_8),
        )
        val publicValue = delegate.step1(identity.decodeToString(), salt.toPositiveBigInteger(), verifier)
        return SrpServerChallenge(salt, publicValue.toUnsignedBytes())
    }

    override fun verify(proof: SrpClientProof): SrpServerProofAndSecret {
        requireNotExpired()
        return mapSrpFailures {
            val evidence = delegate.step2(
                proof.clientPublicValue.toPositiveBigInteger(),
                proof.clientEvidence.toPositiveBigInteger(),
            )
            SrpServerProofAndSecret(
                serverEvidence = evidence.toUnsignedBytes(),
                temporaryKey = delegate.sessionKey.toUnsignedBytes().deriveTemporaryKey(identity),
            )
        }
    }

    private fun requireNotExpired() = requireSrpNotExpired(clock, expiresAt)
}

internal fun ByteArray.toPositiveBigInteger(): BigInteger =
    BigInteger(1, this)

internal fun BigInteger.toUnsignedBytes(): ByteArray =
    BigIntegerUtils.bigIntegerToBytes(this)

private fun ByteArray.deriveTemporaryKey(identity: ByteArray): ByteArray = try {
    HkdfSha256.derive(
        inputKeyMaterial = this,
        salt = MessageDigest.getInstance("SHA-256").digest("bandu-tiji-srp-v1".encodeToByteArray() + identity),
        info = "temporary-authenticated-channel".encodeToByteArray(),
        length = 32,
    )
} finally {
    fill(0)
}

private inline fun <T> mapSrpFailures(block: () -> T): T = try {
    block()
} catch (error: SRP6Exception) {
    throw SrpFailureException(ProtocolErrorCode.PROTOCOL_ERROR_CODE_AUTHENTICATION_FAILED, error)
}

private fun requireSrpNotExpired(clock: Clock, expiresAt: Long) {
    if (clock.nowEpochMillis() >= expiresAt) {
        throw SrpFailureException(ProtocolErrorCode.PROTOCOL_ERROR_CODE_PAIRING_CODE_EXPIRED)
    }
}

private const val SRP_SESSION_MILLIS = 5 * 60 * 1_000L
