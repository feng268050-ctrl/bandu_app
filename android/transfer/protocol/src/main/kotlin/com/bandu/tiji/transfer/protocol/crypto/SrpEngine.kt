package com.bandu.tiji.transfer.protocol.crypto

import com.bandu.tiji.transfer.protocol.proto.ProtocolErrorCode

data class SrpClientHello(
    val identity: ByteArray,
)

data class SrpServerChallenge(
    val salt: ByteArray,
    val serverPublicValue: ByteArray,
)

data class SrpClientProof(
    val clientPublicValue: ByteArray,
    val clientEvidence: ByteArray,
)

data class SrpServerProofAndSecret(
    val serverEvidence: ByteArray,
    val temporaryKey: ByteArray,
)

class SrpFailureException(
    val code: ProtocolErrorCode,
    cause: Throwable? = null,
) : SecurityException(code.name, cause)

interface SrpEngine {
    fun createServer(code: CharArray, identity: ByteArray): SrpServerSession

    fun createClient(code: CharArray, identity: ByteArray): SrpClientSession
}

interface SrpClientSession {
    fun start(): SrpClientHello

    fun answer(challenge: SrpServerChallenge): SrpClientProof

    fun verify(serverEvidence: ByteArray): ByteArray
}

interface SrpServerSession {
    fun challenge(hello: SrpClientHello): SrpServerChallenge

    fun verify(proof: SrpClientProof): SrpServerProofAndSecret
}
