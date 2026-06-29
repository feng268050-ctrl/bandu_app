package com.bandu.tiji.transfer.runtime.pairing

import com.bandu.tiji.domain.transfer.PairingResult
import com.bandu.tiji.domain.transfer.TransferFailureCode
import com.bandu.tiji.domain.transfer.TrustedDevice
import com.bandu.tiji.transfer.protocol.crypto.NimbusSrpEngine
import com.bandu.tiji.transfer.protocol.crypto.SrpEngine
import com.bandu.tiji.transfer.protocol.crypto.SrpFailureException
import com.bandu.tiji.transfer.protocol.identity.IdentityProof
import com.bandu.tiji.transfer.protocol.identity.IdentityVerificationException
import com.bandu.tiji.transfer.protocol.pairing.PairingCodePolicy
import com.bandu.tiji.transfer.protocol.pairing.PairingCodeValidation
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey

class SrpPairingCoordinator(
    private val codePolicy: PairingCodePolicy,
    private val srpEngine: SrpEngine = NimbusSrpEngine(),
) {
    fun pair(
        initiator: PairingDeviceIdentity,
        receiver: PairingDeviceIdentity,
        code: String,
    ): SrpPairingOutcome {
        val validation = codePolicy.validate(code.toCharArray())
        if (validation != PairingCodeValidation.VALID) {
            return SrpPairingOutcome.Failure(validation.toFailureCode())
        }
        return runCatching {
            val identity = srpIdentity(initiator.deviceId, receiver.deviceId)
            val server = srpEngine.createServer(code.toCharArray(), identity)
            val client = srpEngine.createClient(code.toCharArray(), identity)
            val clientHello = client.start()
            val challenge = server.challenge(clientHello)
            val clientProof = client.answer(challenge)
            val serverProof = server.verify(clientProof)
            val initiatorKey = client.verify(serverProof.serverEvidence)
            val receiverKey = serverProof.temporaryKey
            try {
                if (!MessageDigest.isEqual(initiatorKey, receiverKey)) {
                    return SrpPairingOutcome.Failure(TransferFailureCode.PAIRING_FAILED)
                }
                val initiatorExchange = IdentityProof.createExchange(
                    deviceId = initiator.deviceId,
                    displayName = initiator.displayName,
                    signingPublicKey = initiator.signingPublicKey,
                    signingPrivateKey = initiator.signingPrivateKey,
                    authenticatedChannelContext = initiatorKey,
                )
                val receiverExchange = IdentityProof.createExchange(
                    deviceId = receiver.deviceId,
                    displayName = receiver.displayName,
                    signingPublicKey = receiver.signingPublicKey,
                    signingPrivateKey = receiver.signingPrivateKey,
                    authenticatedChannelContext = receiverKey,
                )
                val verifiedReceiver = IdentityProof.verify(receiverExchange, initiatorKey)
                val verifiedInitiator = IdentityProof.verify(initiatorExchange, receiverKey)
                SrpPairingOutcome.Success(
                    initiatorTrust = TrustedDevice(
                        deviceId = verifiedReceiver.deviceId,
                        displayName = verifiedReceiver.displayName,
                        publicKeyFingerprint = verifiedReceiver.fingerprint,
                    ),
                    receiverTrust = TrustedDevice(
                        deviceId = verifiedInitiator.deviceId,
                        displayName = verifiedInitiator.displayName,
                        publicKeyFingerprint = verifiedInitiator.fingerprint,
                    ),
                )
            } finally {
                initiatorKey.fill(0)
                receiverKey.fill(0)
                client.close()
                server.close()
            }
        }.getOrElse { error ->
            when (error) {
                is SrpFailureException ->
                    SrpPairingOutcome.Failure(error.code.toFailureCode())
                is IdentityVerificationException ->
                    SrpPairingOutcome.Failure(TransferFailureCode.PAIRING_FAILED)
                else -> SrpPairingOutcome.Failure(TransferFailureCode.PROTOCOL_ERROR)
            }
        }
    }
}

data class PairingDeviceIdentity(
    val deviceId: String,
    val displayName: String,
    val signingPublicKey: PublicKey,
    val signingPrivateKey: PrivateKey,
)

sealed interface SrpPairingOutcome {
    data class Success(
        val initiatorTrust: TrustedDevice,
        val receiverTrust: TrustedDevice,
    ) : SrpPairingOutcome

    data class Failure(val code: TransferFailureCode) : SrpPairingOutcome
}

fun SrpPairingOutcome.toPairingResult(): PairingResult = when (this) {
    is SrpPairingOutcome.Success -> PairingResult.Success
    is SrpPairingOutcome.Failure -> PairingResult.Failure(code)
}

private fun PairingCodeValidation.toFailureCode(): TransferFailureCode = when (this) {
    PairingCodeValidation.EXPIRED -> TransferFailureCode.PAIRING_EXPIRED
    PairingCodeValidation.INVALID,
    PairingCodeValidation.LOCKED,
    PairingCodeValidation.VALID,
    -> TransferFailureCode.PAIRING_FAILED
}

private fun com.bandu.tiji.transfer.protocol.proto.ProtocolErrorCode.toFailureCode(): TransferFailureCode =
    when (this) {
        com.bandu.tiji.transfer.protocol.proto.ProtocolErrorCode.PROTOCOL_ERROR_CODE_PAIRING_CODE_EXPIRED ->
            TransferFailureCode.PAIRING_EXPIRED
        com.bandu.tiji.transfer.protocol.proto.ProtocolErrorCode.PROTOCOL_ERROR_CODE_CONNECTION_LOST ->
            TransferFailureCode.NETWORK_INTERRUPTED
        else -> TransferFailureCode.PAIRING_FAILED
    }

private fun srpIdentity(
    leftDeviceId: String,
    rightDeviceId: String,
): ByteArray {
    val ordered = listOf(leftDeviceId, rightDeviceId).sorted().joinToString("|")
    return "bandu-tiji-srp-identity-v1|$ordered".encodeToByteArray()
}
