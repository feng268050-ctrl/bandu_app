package com.bandu.tiji.transfer.runtime.identity

import com.bandu.tiji.core.storage.preferences.StoragePreferencesFactory
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.util.ArrayDeque
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeviceIdentityStoreTest {
    @Test
    fun `identity remains stable when preferences and key are reopened`() = runTest {
        val fixture = fixture()
        val first = fixture.store.getOrCreate("Pixel")
        val reopened = DeviceIdentityStore(
            preferences = fixture.preferences,
            keys = fixture.keys,
            createUuid = { error("must not rotate") },
        ).getOrCreate("Ignored")

        assertThat(reopened.deviceId).isEqualTo(first.deviceId)
        assertThat(reopened.signingPublicKey).isEqualTo(first.signingPublicKey)
        assertThat(reopened.publicKeyFingerprint).hasLength(64)
    }

    @Test
    fun `reset rotates uuid key and clears trust records`() = runTest {
        val fixture = fixture("first", "second")
        val first = fixture.store.getOrCreate("Pixel")
        val second = fixture.store.reset("Pixel 2")

        assertThat(second.deviceId).isEqualTo("second")
        assertThat(second.deviceId).isNotEqualTo(first.deviceId)
        assertThat(second.signingPublicKey).isNotEqualTo(first.signingPublicKey)
        assertThat(fixture.keys.load(first.keyAlias)).isNull()
    }

    @Test
    fun `missing keystore entry rotates the complete identity`() = runTest {
        val fixture = fixture("first", "replacement")
        val first = fixture.store.getOrCreate("Pixel")
        fixture.keys.delete(first.keyAlias)

        val replacement = fixture.store.getOrCreate("Pixel")

        assertThat(replacement.deviceId).isEqualTo("replacement")
        assertThat(replacement.keyAlias).isNotEqualTo(first.keyAlias)
    }

    private fun TestScope.fixture(vararg uuids: String): Fixture {
        val root = File("build/test-identity/${System.nanoTime()}")
        val preferences = StoragePreferencesFactory.createDevice(root, this)
        val keys = FakeIdentityKeyStore()
        val queue = ArrayDeque(uuids.toList().ifEmpty { listOf("device-1") })
        return Fixture(
            preferences,
            keys,
            DeviceIdentityStore(preferences, keys) { queue.removeFirst() },
        )
    }

    private data class Fixture(
        val preferences: com.bandu.tiji.core.storage.preferences.DevicePreferencesStore,
        val keys: FakeIdentityKeyStore,
        val store: DeviceIdentityStore,
    )
}

private class FakeIdentityKeyStore : IdentityKeyStore {
    private val entries = mutableMapOf<String, KeyPair>()

    override fun create(alias: String): KeyPair =
        KeyPairGenerator.getInstance("EC").run {
            initialize(ECGenParameterSpec("secp256r1"))
            generateKeyPair()
        }.also { entries[alias] = it }

    override fun load(alias: String): KeyPair? = entries[alias]

    override fun delete(alias: String) {
        entries.remove(alias)
    }
}
