package com.bandu.tiji.transfer.runtime.discovery

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bandu.tiji.domain.transfer.DiscoveryMode
import com.bandu.tiji.transfer.runtime.identity.LocalDeviceIdentity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NsdPeerDiscoveryInstrumentedTest {
    @Test
    fun startAndStopDiscoveryClearsRegisteredServiceAndPeersOnDeviceRuntime() {
        val backend = InstrumentedFakeNsdBackend()
        val discovery = NsdPeerDiscovery(backend)

        discovery.start(localIdentity(), DiscoveryMode.PAIR, port = 41241)
        backend.emit(
            NsdDiscoveredPeer(
                discoveryId = "123456789abc",
                displayName = "Peer",
                mode = DiscoveryMode.PAIR,
                fingerprintPrefix = "abcdef123456",
                hostAddress = "192.168.1.2",
                port = 41241,
            ),
        )

        assertThat(discovery.nearbyDevices.value).hasSize(1)

        discovery.stop()

        assertThat(discovery.nearbyDevices.value).isEmpty()
        assertThat(backend.unregisterCount).isEqualTo(1)
        assertThat(backend.stopDiscoveryCount).isEqualTo(1)
    }

    private fun localIdentity(): LocalDeviceIdentity =
        LocalDeviceIdentity(
            deviceId = "local-device",
            displayName = "Android",
            keyAlias = "alias",
            signingPublicKey = byteArrayOf(1, 2, 3),
            publicKeyFingerprint = "a".repeat(64),
        )
}

private class InstrumentedFakeNsdBackend : NsdDiscoveryBackend {
    var unregisterCount = 0
    var stopDiscoveryCount = 0

    override var onPeerFound: ((NsdDiscoveredPeer) -> Unit)? = null

    override fun register(service: NsdPublishedService) = Unit

    override fun unregister() {
        unregisterCount += 1
    }

    override fun startDiscovery(serviceType: String) = Unit

    override fun stopDiscovery() {
        stopDiscoveryCount += 1
    }

    fun emit(peer: NsdDiscoveredPeer) {
        onPeerFound?.invoke(peer)
    }
}
