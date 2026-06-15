package com.bandu.tiji.transfer.protocol.crypto

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HkdfSha256 {
    private const val HASH_LENGTH = 32

    fun derive(
        inputKeyMaterial: ByteArray,
        salt: ByteArray,
        info: ByteArray,
        length: Int,
    ): ByteArray {
        require(length in 1..(255 * HASH_LENGTH)) { "Invalid HKDF output length" }
        val actualSalt = if (salt.isEmpty()) ByteArray(HASH_LENGTH) else salt
        val pseudoRandomKey = hmac(actualSalt, inputKeyMaterial)
        return try {
            val output = ByteArray(length)
            var previous = ByteArray(0)
            var offset = 0
            var counter = 1
            while (offset < length) {
                val block = hmac(pseudoRandomKey, previous + info + counter.toByte())
                previous.fill(0)
                previous = block
                val count = minOf(block.size, length - offset)
                block.copyInto(output, offset, 0, count)
                offset += count
                counter += 1
            }
            previous.fill(0)
            output
        } finally {
            pseudoRandomKey.fill(0)
        }
    }

    fun deriveAndClearInput(
        inputKeyMaterial: ByteArray,
        salt: ByteArray,
        info: ByteArray,
        length: Int,
    ): ByteArray = try {
        derive(inputKeyMaterial, salt, info, length)
    } finally {
        inputKeyMaterial.fill(0)
    }

    private fun hmac(key: ByteArray, data: ByteArray): ByteArray =
        Mac.getInstance("HmacSHA256").run {
            init(SecretKeySpec(key, "HmacSHA256"))
            doFinal(data)
        }
}
