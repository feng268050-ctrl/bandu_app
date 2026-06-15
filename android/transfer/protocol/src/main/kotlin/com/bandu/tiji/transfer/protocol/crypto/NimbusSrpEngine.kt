package com.bandu.tiji.transfer.protocol.crypto

import com.nimbusds.srp6.BigIntegerUtils
import com.nimbusds.srp6.SRP6ClientSession
import com.nimbusds.srp6.SRP6CryptoParams
import com.nimbusds.srp6.SRP6ServerSession
import com.nimbusds.srp6.SRP6VerifierGenerator
import com.nimbusds.srp6.XRoutineWithUserIdentity
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.SecureRandom

class NimbusSrpEngine(
    private val secureRandom: SecureRandom = SecureRandom(),
) : SrpEngine {
    override fun createServer(code: CharArray, identity: ByteArray): SrpServerSession =
        NimbusServerSession(code.copyOf(), identity.copyOf(), secureRandom)

    override fun createClient(code: CharArray, identity: ByteArray): SrpClientSession =
        NimbusClientSession(code.copyOf(), identity.copyOf())

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
) : SrpClientSession {
    private val delegate = SRP6ClientSession().apply {
        setXRoutine(NimbusSrpEngine.RFC_X_ROUTINE)
    }

    override fun start(): SrpClientHello {
        delegate.step1(identity.decodeToString(), code.concatToString())
        return SrpClientHello(identity.copyOf())
    }

    override fun answer(challenge: SrpServerChallenge): SrpClientProof {
        val credentials = delegate.step2(
            NimbusSrpEngine.PARAMS,
            challenge.salt.toPositiveBigInteger(),
            challenge.serverPublicValue.toPositiveBigInteger(),
        )
        return SrpClientProof(
            clientPublicValue = credentials.A.toUnsignedBytes(),
            clientEvidence = credentials.M1.toUnsignedBytes(),
        )
    }

    override fun verify(serverEvidence: ByteArray): ByteArray {
        delegate.step3(serverEvidence.toPositiveBigInteger())
        return delegate.sessionKey.toUnsignedBytes()
    }
}

private class NimbusServerSession(
    private val code: CharArray,
    private val identity: ByteArray,
    private val secureRandom: SecureRandom,
) : SrpServerSession {
    private val delegate = SRP6ServerSession(NimbusSrpEngine.PARAMS)

    override fun challenge(hello: SrpClientHello): SrpServerChallenge {
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
        val evidence = delegate.step2(
            proof.clientPublicValue.toPositiveBigInteger(),
            proof.clientEvidence.toPositiveBigInteger(),
        )
        return SrpServerProofAndSecret(
            serverEvidence = evidence.toUnsignedBytes(),
            sharedSecret = delegate.sessionKey.toUnsignedBytes(),
        )
    }
}

internal fun ByteArray.toPositiveBigInteger(): BigInteger =
    BigInteger(1, this)

internal fun BigInteger.toUnsignedBytes(): ByteArray =
    BigIntegerUtils.bigIntegerToBytes(this)
