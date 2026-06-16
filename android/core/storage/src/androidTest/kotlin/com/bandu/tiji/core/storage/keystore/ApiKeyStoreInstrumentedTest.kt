package com.bandu.tiji.core.storage.keystore

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.nio.charset.StandardCharsets
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApiKeyStoreInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val encryptedFile = File(
        context.cacheDir,
        "api-key-${System.nanoTime()}/secret.bin",
    )
    private val alias = "bandu_test_api_key_${System.nanoTime()}"
    private val store = ApiKeyStore(encryptedFile, alias)

    @After
    fun tearDown() {
        store.clear()
        encryptedFile.parentFile?.deleteRecursively()
    }

    @Test
    fun apiKeyIsEncryptedAtRestAndCanBeCleared() {
        val secretText = "sk-test-secret-value"
        val secret = secretText.toCharArray()

        store.save(secret)

        assertThat(secret).isEqualTo(CharArray(secretText.length))
        assertThat(encryptedFile.isFile).isTrue()
        assertThat(encryptedFile.readText(StandardCharsets.ISO_8859_1))
            .doesNotContain(secretText)
        assertThat(store.read()?.concatToString()).isEqualTo(secretText)

        store.clear()

        assertThat(encryptedFile.exists()).isFalse()
        assertThat(store.read()).isNull()
    }

    @Test
    fun replacingApiKeyDoesNotLeavePlaintextOrTempFile() {
        val first = "first-secret".toCharArray()
        val secondText = "second-secret"

        store.save(first)
        store.save(secondText.toCharArray())

        assertThat(store.read()?.concatToString()).isEqualTo(secondText)
        assertThat(File(encryptedFile.parentFile, "${encryptedFile.name}.tmp").exists())
            .isFalse()
        assertThat(encryptedFile.readText(StandardCharsets.ISO_8859_1))
            .doesNotContain("first-secret")
        assertThat(encryptedFile.readText(StandardCharsets.ISO_8859_1))
            .doesNotContain(secondText)
    }
}
