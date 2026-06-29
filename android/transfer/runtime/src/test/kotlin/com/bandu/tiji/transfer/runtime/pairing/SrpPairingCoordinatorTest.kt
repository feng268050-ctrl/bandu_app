package com.bandu.tiji.transfer.runtime.pairing

import com.bandu.tiji.core.common.time.Clock
import com.bandu.tiji.domain.transfer.TransferFailureCode
import com.bandu.tiji.transfer.protocol.identity.IdentityProof
import com.bandu.tiji.transfer.protocol.pairing.PairingCodePolicy
import com.google.common.truth.Truth.assertThat
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SrpPairingCoordinatorTest {
    @Test
    fun `valid code establishes mirrored trusted identities through srp`() = runTest {
        val clock = MutableClock(1_000L)
        val policy = PairingCodePolicy(clock, DeterministicSecureRandom("123456"))
        val code = requireNotNull(policy.issue()).value.concatToString()
        val initiator = identity("source-device", "旧手机")
        val receiver = identity("target-device", "新手机")

        val outcome = SrpPairingCoordinator(policy).pair(initiator, receiver, code)

        assertThat(outcome).isInstanceOf(SrpPairingOutcome.Success::class.java)
        val success = outcome as SrpPairingOutcome.Success
        assertThat(success.initiatorTrust.deviceId).isEqualTo("target-device")
        assertThat(success.initiatorTrust.displayName).isEqualTo("新手机")
        assertThat(success.initiatorTrust.publicKeyFingerprint)
            .isEqualTo(IdentityProof.fingerprint(receiver.signingPublicKey.encoded))
        assertThat(success.receiverTrust.deviceId).isEqualTo("source-device")
        assertThat(success.receiverTrust.publicKeyFingerprint)
            .isEqualTo(IdentityProof.fingerprint(initiator.signingPublicKey.encoded))
    }

    @Test
    fun `wrong code is rejected before trust is created`() = runTest {
        val policy = PairingCodePolicy(MutableClock(1_000L), DeterministicSecureRandom("123456"))
        requireNotNull(policy.issue())

        val outcome = SrpPairingCoordinator(policy).pair(
            identity("source-device", "旧手机"),
            identity("target-device", "新手机"),
            "654321",
        )

        assertThat(outcome).isEqualTo(SrpPairingOutcome.Failure(TransferFailureCode.PAIRING_FAILED))
    }

    @Test
    fun `expired code maps to pairing expired`() = runTest {
        val clock = MutableClock(1_000L)
        val policy = PairingCodePolicy(clock, DeterministicSecureRandom("123456"))
        val code = requireNotNull(policy.issue()).value.concatToString()
        clock.advance(PairingCodePolicy.VALIDITY_MILLIS + 1)

        val outcome = SrpPairingCoordinator(policy).pair(
            identity("source-device", "旧手机"),
            identity("target-device", "新手机"),
            code,
        )

        assertThat(outcome).isEqualTo(SrpPairingOutcome.Failure(TransferFailureCode.PAIRING_EXPIRED))
    }

    private fun identity(
        deviceId: String,
        displayName: String,
    ): PairingDeviceIdentity {
        val keyPair = KeyPairGenerator.getInstance("EC").run {
            initialize(ECGenParameterSpec("secp256r1"))
            generateKeyPair()
        }
        return PairingDeviceIdentity(
            deviceId = deviceId,
            displayName = displayName,
            signingPublicKey = keyPair.public,
            signingPrivateKey = keyPair.private,
        )
    }
}

private class MutableClock(private var now: Long) : Clock {
    override fun nowEpochMillis(): Long = now

    fun advance(millis: Long) {
        now += millis
    }
}

private class DeterministicSecureRandom(
    private val digits: String,
) : java.security.SecureRandom() {
    private var index = 0

    override fun nextInt(bound: Int): Int {
        val digit = digits[index % digits.length].digitToInt()
        index += 1
        return digit % bound
    }
}
