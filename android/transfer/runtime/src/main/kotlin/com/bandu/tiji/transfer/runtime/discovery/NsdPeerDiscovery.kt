package com.bandu.tiji.transfer.runtime.discovery

import com.bandu.tiji.domain.transfer.DiscoveryMode
import com.bandu.tiji.domain.transfer.NearbyDevice
import com.bandu.tiji.transfer.protocol.identity.IdentityProof
import com.bandu.tiji.transfer.runtime.TransferRuntimeConfig
import com.bandu.tiji.transfer.runtime.identity.LocalDeviceIdentity
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NsdPeerDiscovery(
    private val backend: NsdDiscoveryBackend,
    private val serviceType: String = TransferRuntimeConfig.SERVICE_TYPE,
) {
    private val peers = MutableStateFlow<List<NsdDiscoveredPeer>>(emptyList())
    private val nearby = MutableStateFlow<List<NearbyDevice>>(emptyList())
    private var started = false
    private var localDeviceDigest: String? = null

    val nearbyDevices: StateFlow<List<NearbyDevice>> = nearby.asStateFlow()

    init {
        backend.onPeerFound = { peer ->
            if (started) {
                val localDigest = localDeviceDigest
                val current = peers.value.filterNot { it.discoveryId == peer.discoveryId }
                val next = if (peer.discoveryId == localDigest) current else current + peer
                peers.value = next
                nearby.value = next.map { it.toNearbyDevice() }
            }
        }
    }

    fun start(
        localIdentity: LocalDeviceIdentity,
        mode: DiscoveryMode,
        port: Int,
    ) {
        require(port in 1..65_535) { "NSD port must be within TCP port range" }
        if (started) stop()
        val deviceDigest = localIdentity.deviceId.discoveryDigest()
        localDeviceDigest = deviceDigest
        started = true
        val displayName = localIdentity.displayName.limitUtf8Bytes(TransferRuntimeConfig.MAX_DEVICE_NAME_BYTES)
        backend.register(
            NsdPublishedService(
                serviceType = serviceType,
                serviceName = "BanduTiji-${deviceDigest.take(8)}",
                port = port,
                txt = mapOf(
                    TXT_VERSION to PROTOCOL_VERSION,
                    TXT_DEVICE to deviceDigest,
                    TXT_NAME to displayName,
                    TXT_MODE to mode.toTxtValue(),
                    TXT_FINGERPRINT to localIdentity.publicKeyFingerprint.take(12),
                ),
            ),
        )
        backend.startDiscovery(serviceType)
    }

    fun stop() {
        started = false
        localDeviceDigest = null
        backend.stopDiscovery()
        backend.unregister()
        peers.value = emptyList()
        nearby.value = emptyList()
    }

    companion object {
        const val TXT_VERSION = "v"
        const val TXT_DEVICE = "device"
        const val TXT_NAME = "name"
        const val TXT_MODE = "mode"
        const val TXT_FINGERPRINT = "fingerprint"
        const val PROTOCOL_VERSION = "1"
    }
}

interface NsdDiscoveryBackend {
    var onPeerFound: ((NsdDiscoveredPeer) -> Unit)?

    fun register(service: NsdPublishedService)

    fun unregister()

    fun startDiscovery(serviceType: String)

    fun stopDiscovery()
}

data class NsdPublishedService(
    val serviceType: String,
    val serviceName: String,
    val port: Int,
    val txt: Map<String, String>,
) {
    init {
        require(serviceType == TransferRuntimeConfig.SERVICE_TYPE)
        require(serviceName.startsWith("BanduTiji-"))
        require(port in 1..65_535)
        require(txt[NsdPeerDiscovery.TXT_VERSION] == NsdPeerDiscovery.PROTOCOL_VERSION)
        require(!txt.containsKey("code")) { "Pairing code must not be advertised through NSD TXT" }
    }
}

data class NsdDiscoveredPeer(
    val discoveryId: String,
    val displayName: String,
    val mode: DiscoveryMode,
    val fingerprintPrefix: String,
    val hostAddress: String,
    val port: Int,
) {
    init {
        require(discoveryId.isNotBlank())
        require(displayName.isNotBlank())
        require(hostAddress.isNotBlank())
        require(port in 1..65_535)
    }
}

fun parseNsdPeer(
    serviceType: String,
    txt: Map<String, String>,
    hostAddress: String,
    port: Int,
): NsdDiscoveredPeer? {
    if (serviceType != TransferRuntimeConfig.SERVICE_TYPE) return null
    if (txt[NsdPeerDiscovery.TXT_VERSION] != NsdPeerDiscovery.PROTOCOL_VERSION) return null
    val device = txt[NsdPeerDiscovery.TXT_DEVICE]?.takeIf { it.length == 12 } ?: return null
    val name = txt[NsdPeerDiscovery.TXT_NAME]?.takeIf { it.isNotBlank() } ?: return null
    val mode = when (txt[NsdPeerDiscovery.TXT_MODE]) {
        "pair" -> DiscoveryMode.PAIR
        "trusted" -> DiscoveryMode.TRUSTED
        else -> return null
    }
    val fingerprint = txt[NsdPeerDiscovery.TXT_FINGERPRINT]?.takeIf { it.length == 12 } ?: return null
    return NsdDiscoveredPeer(
        discoveryId = device,
        displayName = name.limitUtf8Bytes(TransferRuntimeConfig.MAX_DEVICE_NAME_BYTES),
        mode = mode,
        fingerprintPrefix = fingerprint,
        hostAddress = hostAddress,
        port = port,
    )
}

internal fun NsdDiscoveredPeer.toNearbyDevice(): NearbyDevice =
    NearbyDevice(
        discoveryId = discoveryId,
        displayName = displayName,
        mode = mode,
    )

internal fun DiscoveryMode.toTxtValue(): String = when (this) {
    DiscoveryMode.PAIR -> "pair"
    DiscoveryMode.TRUSTED -> "trusted"
}

internal fun String.discoveryDigest(): String =
    IdentityProof.fingerprint(toByteArray(StandardCharsets.UTF_8)).take(12)

internal fun String.limitUtf8Bytes(maxBytes: Int): String {
    require(maxBytes > 0)
    var end = length
    while (end > 0 && substring(0, end).toByteArray(StandardCharsets.UTF_8).size > maxBytes) {
        end -= 1
    }
    return substring(0, end).ifBlank { "Android" }
}
