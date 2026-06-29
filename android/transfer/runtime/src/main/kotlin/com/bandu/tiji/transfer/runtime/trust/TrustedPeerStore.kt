package com.bandu.tiji.transfer.runtime.trust

import com.bandu.tiji.core.storage.preferences.DevicePreferencesStore
import com.bandu.tiji.domain.transfer.TrustedDevice
import java.net.URLDecoder
import java.net.URLEncoder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface TrustedPeerStore {
    fun observeTrustedPeers(): Flow<List<TrustedDevice>>

    suspend fun upsert(peer: TrustedDevice)

    suspend fun remove(deviceId: String)

    suspend fun find(deviceId: String): TrustedDevice?
}

class DevicePreferencesTrustedPeerStore(
    private val preferences: DevicePreferencesStore,
) : TrustedPeerStore {
    override fun observeTrustedPeers(): Flow<List<TrustedDevice>> =
        preferences.data.map { devicePreferences ->
            devicePreferences.trustedPeerRecords.mapNotNull(::decodeTrustedPeer)
                .sortedBy { it.displayName }
        }

    override suspend fun upsert(peer: TrustedDevice) {
        val current = preferences.data.first()
        val peers = current.trustedPeerRecords.mapNotNull(::decodeTrustedPeer)
            .filterNot { it.deviceId == peer.deviceId } + peer
        preferences.replace(current.copy(trustedPeerRecords = peers.map(::encodeTrustedPeer).toSet()))
    }

    override suspend fun remove(deviceId: String) {
        val current = preferences.data.first()
        val peers = current.trustedPeerRecords.mapNotNull(::decodeTrustedPeer)
            .filterNot { it.deviceId == deviceId }
        preferences.replace(current.copy(trustedPeerRecords = peers.map(::encodeTrustedPeer).toSet()))
    }

    override suspend fun find(deviceId: String): TrustedDevice? =
        preferences.data.first().trustedPeerRecords
            .mapNotNull(::decodeTrustedPeer)
            .firstOrNull { it.deviceId == deviceId }
}

class InMemoryTrustedPeerStore(
    initialPeers: List<TrustedDevice> = emptyList(),
) : TrustedPeerStore {
    private val trustedPeers = kotlinx.coroutines.flow.MutableStateFlow(initialPeers)

    override fun observeTrustedPeers(): Flow<List<TrustedDevice>> = trustedPeers

    override suspend fun upsert(peer: TrustedDevice) {
        trustedPeers.value = trustedPeers.value.filterNot { it.deviceId == peer.deviceId } + peer
    }

    override suspend fun remove(deviceId: String) {
        trustedPeers.value = trustedPeers.value.filterNot { it.deviceId == deviceId }
    }

    override suspend fun find(deviceId: String): TrustedDevice? =
        trustedPeers.value.firstOrNull { it.deviceId == deviceId }
}

internal fun encodeTrustedPeer(peer: TrustedDevice): String =
    listOf(
        peer.deviceId,
        peer.displayName.encodeComponent(),
        peer.publicKeyFingerprint,
    ).joinToString(SEPARATOR)

internal fun decodeTrustedPeer(record: String): TrustedDevice? {
    val parts = record.split(SEPARATOR)
    if (parts.size != 3) return null
    val fingerprint = parts[2]
    if (parts[0].isBlank() || fingerprint.length != 64) return null
    return TrustedDevice(
        deviceId = parts[0],
        displayName = parts[1].decodeComponent().ifBlank { "Android" },
        publicKeyFingerprint = fingerprint,
    )
}

private fun String.encodeComponent(): String =
    URLEncoder.encode(this, Charsets.UTF_8.name())

private fun String.decodeComponent(): String =
    runCatching { URLDecoder.decode(this, Charsets.UTF_8.name()) }.getOrDefault("")

private const val SEPARATOR = "|"
