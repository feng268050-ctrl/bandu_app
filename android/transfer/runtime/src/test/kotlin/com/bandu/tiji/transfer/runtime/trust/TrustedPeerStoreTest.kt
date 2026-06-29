package com.bandu.tiji.transfer.runtime.trust

import com.bandu.tiji.core.storage.preferences.StoragePreferencesFactory
import com.bandu.tiji.domain.transfer.TrustedDevice
import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TrustedPeerStoreTest {
    @Test
    fun `trusted peers persist in device preferences and can be forgotten`() = runTest {
        val root = File("build/test-trusted-peers/${System.nanoTime()}")
        val preferences = StoragePreferencesFactory.createDevice(root, this)
        val store = DevicePreferencesTrustedPeerStore(preferences)
        val peer = TrustedDevice(
            deviceId = "device-a",
            displayName = "客厅平板",
            publicKeyFingerprint = "a".repeat(64),
        )

        store.upsert(peer)
        val reopened = DevicePreferencesTrustedPeerStore(preferences)

        assertThat(reopened.observeTrustedPeers().first()).containsExactly(peer)
        assertThat(reopened.find("device-a")).isEqualTo(peer)

        reopened.remove("device-a")

        assertThat(store.observeTrustedPeers().first()).isEmpty()
    }

    @Test
    fun `upsert replaces matching identity instead of duplicating`() = runTest {
        val store = inMemoryStore()

        store.upsert(TrustedDevice("device-a", "旧名称", "a".repeat(64)))
        store.upsert(TrustedDevice("device-a", "新名称", "b".repeat(64)))

        assertThat(store.observeTrustedPeers().first()).containsExactly(
            TrustedDevice("device-a", "新名称", "b".repeat(64)),
        )
    }

    @Test
    fun `invalid records are ignored`() {
        assertThat(decodeTrustedPeer("bad")).isNull()
        assertThat(decodeTrustedPeer("device|name|short")).isNull()
        assertThat(
            decodeTrustedPeer(
                encodeTrustedPeer(TrustedDevice("device-a", "平板", "c".repeat(64))),
            ),
        ).isEqualTo(TrustedDevice("device-a", "平板", "c".repeat(64)))
    }

    private fun TestScope.inMemoryStore(): TrustedPeerStore = InMemoryTrustedPeerStore()
}
