package com.bandu.tiji.transfer.runtime.discovery

import com.bandu.tiji.domain.transfer.DiscoveryMode
import com.bandu.tiji.transfer.runtime.TransferRuntimeConfig
import com.bandu.tiji.transfer.runtime.identity.LocalDeviceIdentity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class NsdPeerDiscoveryTest {
    @Test
    fun `start registers bandu nsd service with safe txt and starts scan`() = runTest {
        val backend = FakeNsdDiscoveryBackend()
        val discovery = NsdPeerDiscovery(backend)

        discovery.start(localIdentity(displayName = "很长很长很长很长很长的设备名称"), DiscoveryMode.PAIR, port = 43123)

        val published = backend.registered.single()
        assertThat(published.serviceType).isEqualTo(TransferRuntimeConfig.SERVICE_TYPE)
        assertThat(published.serviceName).startsWith("BanduTiji-")
        assertThat(published.port).isEqualTo(43123)
        assertThat(published.txt[NsdPeerDiscovery.TXT_VERSION]).isEqualTo("1")
        assertThat(published.txt[NsdPeerDiscovery.TXT_DEVICE]).hasLength(12)
        assertThat(published.txt[NsdPeerDiscovery.TXT_MODE]).isEqualTo("pair")
        assertThat(published.txt[NsdPeerDiscovery.TXT_FINGERPRINT]).isEqualTo("abcdef123456")
        assertThat(published.txt.keys).doesNotContain("code")
        assertThat(published.txt.getValue(NsdPeerDiscovery.TXT_NAME).toByteArray(Charsets.UTF_8).size)
            .isAtMost(TransferRuntimeConfig.MAX_DEVICE_NAME_BYTES)
        assertThat(backend.startedServiceTypes).containsExactly(TransferRuntimeConfig.SERVICE_TYPE)
    }

    @Test
    fun `manual discovery ignores peers before start and after stop`() = runTest {
        val backend = FakeNsdDiscoveryBackend()
        val discovery = NsdPeerDiscovery(backend)
        val peer = peer(discoveryId = "123456789abc")

        backend.emit(peer)
        assertThat(discovery.nearbyDevices.value).isEmpty()

        discovery.start(localIdentity(), DiscoveryMode.TRUSTED, port = 40000)
        backend.emit(peer)
        assertThat(discovery.nearbyDevices.value).containsExactly(
            com.bandu.tiji.domain.transfer.NearbyDevice(
                discoveryId = "123456789abc",
                displayName = "Peer",
                mode = DiscoveryMode.TRUSTED,
            ),
        )

        discovery.stop()
        assertThat(discovery.nearbyDevices.value).isEmpty()
        assertThat(backend.stopDiscoveryCount).isEqualTo(1)
        assertThat(backend.unregisterCount).isEqualTo(1)

        backend.emit(peer(discoveryId = "abcdef123456"))
        assertThat(discovery.nearbyDevices.value).isEmpty()
    }

    @Test
    fun `manual endpoint adds private peer only while discovery is active`() = runTest {
        val backend = FakeNsdDiscoveryBackend()
        val discovery = NsdPeerDiscovery(backend)
        val endpoint = parseManualDiscoveryEndpoint("192.168.1.50:41242", defaultPort = 41241)

        discovery.addManualPeer(endpoint)
        assertThat(discovery.nearbyDevices.value).isEmpty()

        discovery.start(localIdentity(), DiscoveryMode.PAIR, port = 41241)
        discovery.addManualPeer(endpoint)
        discovery.addManualPeer(endpoint.toPeer(displayName = "旧手机"))

        assertThat(discovery.nearbyDevices.value).containsExactly(
            com.bandu.tiji.domain.transfer.NearbyDevice(
                discoveryId = endpoint.toPeer(displayName = "旧手机").discoveryId,
                displayName = "旧手机",
                mode = DiscoveryMode.PAIR,
            ),
        )

        discovery.stop()

        assertThat(discovery.nearbyDevices.value).isEmpty()
    }

    @Test
    fun `manual endpoint parser accepts local addresses and rejects public targets`() {
        assertThat(parseManualDiscoveryEndpoint("10.0.2.2", defaultPort = 41241))
            .isEqualTo(ManualDiscoveryEndpoint("10.0.2.2", 41241))
        assertThat(parseManualDiscoveryEndpoint("172.16.0.4:50000", defaultPort = 41241))
            .isEqualTo(ManualDiscoveryEndpoint("172.16.0.4", 50000))
        assertThat(parseManualDiscoveryEndpoint("[fd00::1]:41242", defaultPort = 41241))
            .isEqualTo(ManualDiscoveryEndpoint("fd00::1", 41242))
        assertThat(parseManualDiscoveryEndpoint("fe80::1", defaultPort = 41241))
            .isEqualTo(ManualDiscoveryEndpoint("fe80::1", 41241))

        assertThat(runCatching { parseManualDiscoveryEndpoint("8.8.8.8", 41241) }.exceptionOrNull())
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThat(runCatching { parseManualDiscoveryEndpoint("example.com", 41241) }.exceptionOrNull())
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThat(runCatching { parseManualDiscoveryEndpoint("192.168.1.2:70000", 41241) }.exceptionOrNull())
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `parse rejects wrong service unsupported version and invalid mode`() {
        val txt = mapOf(
            NsdPeerDiscovery.TXT_VERSION to "1",
            NsdPeerDiscovery.TXT_DEVICE to "123456789abc",
            NsdPeerDiscovery.TXT_NAME to "Peer",
            NsdPeerDiscovery.TXT_MODE to "pair",
            NsdPeerDiscovery.TXT_FINGERPRINT to "abcdef123456",
        )

        assertThat(parseNsdPeer("_other._tcp.", txt, "192.168.1.2", 44321)).isNull()
        assertThat(
            parseNsdPeer(
                TransferRuntimeConfig.SERVICE_TYPE,
                txt + (NsdPeerDiscovery.TXT_VERSION to "2"),
                "192.168.1.2",
                44321,
            ),
        ).isNull()
        assertThat(
            parseNsdPeer(
                TransferRuntimeConfig.SERVICE_TYPE,
                txt + (NsdPeerDiscovery.TXT_MODE to "sync"),
                "192.168.1.2",
                44321,
            ),
        ).isNull()
    }

    private fun localIdentity(displayName: String = "Pixel"): LocalDeviceIdentity =
        LocalDeviceIdentity(
            deviceId = "local-device-id",
            displayName = displayName,
            keyAlias = "alias",
            signingPublicKey = byteArrayOf(1, 2, 3),
            publicKeyFingerprint = "abcdef1234567890",
        )

    private fun peer(discoveryId: String): NsdDiscoveredPeer =
        NsdDiscoveredPeer(
            discoveryId = discoveryId,
            displayName = "Peer",
            mode = DiscoveryMode.TRUSTED,
            fingerprintPrefix = "fedcba654321",
            hostAddress = "192.168.1.2",
            port = 44321,
        )
}

private class FakeNsdDiscoveryBackend : NsdDiscoveryBackend {
    val registered = mutableListOf<NsdPublishedService>()
    val startedServiceTypes = mutableListOf<String>()
    var stopDiscoveryCount = 0
    var unregisterCount = 0

    override var onPeerFound: ((NsdDiscoveredPeer) -> Unit)? = null

    override fun register(service: NsdPublishedService) {
        registered += service
    }

    override fun unregister() {
        unregisterCount += 1
    }

    override fun startDiscovery(serviceType: String) {
        startedServiceTypes += serviceType
    }

    override fun stopDiscovery() {
        stopDiscoveryCount += 1
    }

    fun emit(peer: NsdDiscoveredPeer) {
        onPeerFound?.invoke(peer)
    }
}
