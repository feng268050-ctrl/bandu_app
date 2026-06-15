package com.bandu.tiji.transfer.protocol.pairing

import com.bandu.tiji.core.common.time.Clock
import java.security.SecureRandom

data class IssuedPairingCode(
    val value: CharArray,
    val expiresAtEpochMillis: Long,
)

enum class PairingCodeValidation {
    VALID,
    INVALID,
    EXPIRED,
    LOCKED,
}

class PairingCodePolicy(
    private val clock: Clock,
    private val secureRandom: SecureRandom = SecureRandom(),
) {
    private var activeCode: CharArray? = null
    private var expiresAtEpochMillis: Long = 0
    private var failedAttempts: Int = 0
    private var cooldownUntilEpochMillis: Long = 0

    fun issue(): IssuedPairingCode? {
        val now = clock.nowEpochMillis()
        if (now < cooldownUntilEpochMillis) return null

        clearActiveCode()
        val code = CharArray(CODE_LENGTH) {
            ('0'.code + secureRandom.nextInt(10)).toChar()
        }
        activeCode = code
        expiresAtEpochMillis = now + VALIDITY_MILLIS
        failedAttempts = 0
        return IssuedPairingCode(code.copyOf(), expiresAtEpochMillis)
    }

    fun validate(candidate: CharArray): PairingCodeValidation {
        return try {
            val now = clock.nowEpochMillis()
            if (now < cooldownUntilEpochMillis) return PairingCodeValidation.LOCKED

            val expected = activeCode ?: return PairingCodeValidation.EXPIRED
            if (now >= expiresAtEpochMillis) {
                clearActiveCode()
                return PairingCodeValidation.EXPIRED
            }

            if (constantTimeEquals(expected, candidate)) {
                failedAttempts = 0
                clearActiveCode()
                return PairingCodeValidation.VALID
            }

            failedAttempts += 1
            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                clearActiveCode()
                cooldownUntilEpochMillis = now + COOLDOWN_MILLIS
                return PairingCodeValidation.LOCKED
            }
            PairingCodeValidation.INVALID
        } finally {
            candidate.fill('\u0000')
        }
    }

    fun invalidate() {
        clearActiveCode()
        failedAttempts = 0
        expiresAtEpochMillis = 0
    }

    fun remainingCooldownMillis(): Long =
        (cooldownUntilEpochMillis - clock.nowEpochMillis()).coerceAtLeast(0)

    private fun constantTimeEquals(expected: CharArray, candidate: CharArray): Boolean {
        var difference = expected.size xor candidate.size
        for (index in expected.indices) {
            val candidateCode = candidate.getOrNull(index)?.code ?: 0
            difference = difference or (expected[index].code xor candidateCode)
        }
        return difference == 0
    }

    private fun clearActiveCode() {
        activeCode?.fill('\u0000')
        activeCode = null
    }

    companion object {
        const val CODE_LENGTH = 6
        const val MAX_FAILED_ATTEMPTS = 5
        const val VALIDITY_MILLIS = 5 * 60 * 1_000L
        const val COOLDOWN_MILLIS = 60 * 1_000L
    }
}
