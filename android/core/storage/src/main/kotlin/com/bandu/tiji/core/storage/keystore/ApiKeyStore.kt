package com.bandu.tiji.core.storage.keystore

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class ApiKeyStore(
    private val encryptedFile: File,
    private val alias: String = DEFAULT_ALIAS,
) {
    fun save(apiKey: CharArray) {
        require(apiKey.isNotEmpty()) { "API key must not be empty" }
        val plaintext = apiKey.concatToString().toByteArray(StandardCharsets.UTF_8)
        apiKey.fill('\u0000')
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val ciphertext = cipher.doFinal(plaintext)
            writeAtomically(encode(cipher.iv, ciphertext))
        } finally {
            plaintext.fill(0)
        }
    }

    fun read(): CharArray? {
        if (!encryptedFile.isFile) return null
        val record = decode(encryptedFile.readBytes())
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getExistingKey() ?: return null,
            GCMParameterSpec(GCM_TAG_BITS, record.iv),
        )
        val plaintext = cipher.doFinal(record.ciphertext)
        return try {
            plaintext.toString(StandardCharsets.UTF_8).toCharArray()
        } finally {
            plaintext.fill(0)
        }
    }

    fun clear() {
        if (encryptedFile.exists()) {
            encryptedFile.delete()
        }
        keyStore().let { store ->
            if (store.containsAlias(alias)) {
                store.deleteEntry(alias)
            }
        }
    }

    private fun getExistingKey(): SecretKey? =
        keyStore().getKey(alias, null) as? SecretKey

    private fun getOrCreateKey(): SecretKey {
        getExistingKey()?.let { return it }
        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE,
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setKeySize(KEY_SIZE_BITS)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private fun keyStore(): KeyStore =
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun writeAtomically(bytes: ByteArray) {
        require(encryptedFile.parentFile?.let { it.exists() || it.mkdirs() } != false) {
            "Unable to create API key directory"
        }
        val tempFile = File(encryptedFile.parentFile, "${encryptedFile.name}.tmp")
        tempFile.writeBytes(bytes)
        if (encryptedFile.exists() && !encryptedFile.delete()) {
            tempFile.delete()
            error("Unable to replace encrypted API key")
        }
        if (!tempFile.renameTo(encryptedFile)) {
            tempFile.delete()
            error("Unable to commit encrypted API key")
        }
    }

    private fun encode(
        iv: ByteArray,
        ciphertext: ByteArray,
    ): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { output ->
            output.writeInt(MAGIC)
            output.writeInt(VERSION)
            output.writeInt(iv.size)
            output.write(iv)
            output.writeInt(ciphertext.size)
            output.write(ciphertext)
        }
        bytes.toByteArray()
    }

    private fun decode(bytes: ByteArray): EncryptedApiKeyRecord =
        DataInputStream(ByteArrayInputStream(bytes)).use { input ->
            if (input.readInt() != MAGIC || input.readInt() != VERSION) {
                throw ApiKeyStorageException("Unsupported API key record")
            }
            val iv = input.readSizedBytes(MAX_IV_BYTES)
            val ciphertext = input.readSizedBytes(MAX_CIPHERTEXT_BYTES)
            EncryptedApiKeyRecord(iv, ciphertext)
        }

    private fun DataInputStream.readSizedBytes(maxSize: Int): ByteArray {
        val size = readInt()
        if (size <= 0 || size > maxSize) {
            throw ApiKeyStorageException("Invalid API key record")
        }
        return ByteArray(size).also(::readFully)
    }

    private data class EncryptedApiKeyRecord(
        val iv: ByteArray,
        val ciphertext: ByteArray,
    )

    companion object {
        const val DEFAULT_ALIAS = "bandu_tiji_api_key_v1"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BITS = 256
        private const val GCM_TAG_BITS = 128
        private const val MAGIC = 0x4254414b
        private const val VERSION = 1
        private const val MAX_IV_BYTES = 32
        private const val MAX_CIPHERTEXT_BYTES = 32 * 1024
    }
}

class ApiKeyStorageException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
