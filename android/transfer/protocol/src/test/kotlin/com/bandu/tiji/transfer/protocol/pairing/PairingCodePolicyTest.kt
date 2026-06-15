package com.bandu.tiji.transfer.protocol.pairing

import com.bandu.tiji.core.common.time.Clock
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.security.SecureRandom

class PairingCodePolicyTest {
    private val clock = MutableClock(1_000)
    private val policy = PairingCodePolicy(clock, FixedSecureRandom(intArrayOf(0, 1, 2, 3, 4, 5)))

    @Test
    fun `issues exactly six digits including leading zero for five minutes`() {
        val issued = requireNotNull(policy.issue())
        val candidate = "012345".toCharArray()

        assertThat(issued.value.concatToString()).isEqualTo("012345")
        assertThat(issued.expiresAtEpochMillis).isEqualTo(1_000 + PairingCodePolicy.VALIDITY_MILLIS)
        assertThat(policy.validate(candidate)).isEqualTo(PairingCodeValidation.VALID)
        assertThat(candidate).isEqualTo(CharArray(6))
        assertThat(policy.validate("012345".toCharArray()))
            .isEqualTo(PairingCodeValidation.EXPIRED)
    }

    @Test
    fun `code expires exactly at five minutes`() {
        val issued = requireNotNull(policy.issue())
        clock.advance(PairingCodePolicy.VALIDITY_MILLIS)

        assertThat(policy.validate(issued.value)).isEqualTo(PairingCodeValidation.EXPIRED)
    }

    @Test
    fun `fifth failure invalidates code and starts sixty second cooldown`() {
        policy.issue()

        repeat(4) {
            assertThat(policy.validate("999999".toCharArray())).isEqualTo(PairingCodeValidation.INVALID)
        }
        assertThat(policy.validate("999999".toCharArray())).isEqualTo(PairingCodeValidation.LOCKED)
        assertThat(policy.issue()).isNull()
        assertThat(policy.remainingCooldownMillis()).isEqualTo(PairingCodePolicy.COOLDOWN_MILLIS)

        clock.advance(PairingCodePolicy.COOLDOWN_MILLIS)
        assertThat(policy.issue()).isNotNull()
    }

    @Test
    fun `invalid expired and locked candidate buffers are zeroed`() {
        policy.issue()
        val invalid = "999999".toCharArray()
        assertThat(policy.validate(invalid)).isEqualTo(PairingCodeValidation.INVALID)
        assertThat(invalid).isEqualTo(CharArray(6))

        clock.advance(PairingCodePolicy.VALIDITY_MILLIS)
        val expired = "012345".toCharArray()
        assertThat(policy.validate(expired)).isEqualTo(PairingCodeValidation.EXPIRED)
        assertThat(expired).isEqualTo(CharArray(6))

        policy.issue()
        repeat(4) { policy.validate("999999".toCharArray()) }
        policy.validate("999999".toCharArray())
        val locked = "123456".toCharArray()
        assertThat(policy.validate(locked)).isEqualTo(PairingCodeValidation.LOCKED)
        assertThat(locked).isEqualTo(CharArray(6))
    }

    private class MutableClock(private var now: Long) : Clock {
        override fun nowEpochMillis(): Long = now

        fun advance(millis: Long) {
            now += millis
        }
    }

    private class FixedSecureRandom(private val values: IntArray) : SecureRandom() {
        private var index = 0

        override fun nextInt(bound: Int): Int = values[index++ % values.size] % bound
    }
}
