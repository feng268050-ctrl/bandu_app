package com.bandu.tiji.transfer.protocol.crypto

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HkdfSha256Test {
    @Test
    fun `matches RFC 5869 test case one`() {
        val output = HkdfSha256.derive(
            inputKeyMaterial = ByteArray(22) { 0x0b },
            salt = "000102030405060708090a0b0c".hexToByteArray(),
            info = "f0f1f2f3f4f5f6f7f8f9".hexToByteArray(),
            length = 42,
        )

        assertThat(output.toHex())
            .isEqualTo("3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865")
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
