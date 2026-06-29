package com.bandu.tiji.transfer.runtime

import com.bandu.tiji.core.common.time.Clock
import com.bandu.tiji.core.common.time.SystemClock
import com.bandu.tiji.domain.transfer.DiscoveryMode
import com.bandu.tiji.domain.transfer.NearbyDevice
import com.bandu.tiji.domain.transfer.PairingCode
import com.bandu.tiji.domain.transfer.PairingResult
import com.bandu.tiji.domain.transfer.TransferFailureCode
import com.bandu.tiji.domain.transfer.TransferOfferSummary
import com.bandu.tiji.domain.transfer.TransferState
import com.bandu.tiji.domain.transfer.TransferSummary
import com.bandu.tiji.domain.transfer.TrustedDevice
import com.bandu.tiji.core.model.transfer.TransferPhase
import com.bandu.tiji.core.model.transfer.TransferProgress
import com.bandu.tiji.transfer.protocol.identity.IdentityProof
import com.bandu.tiji.transfer.protocol.pairing.PairingCodePolicy
import com.bandu.tiji.transfer.protocol.pairing.PairingCodeValidation
import com.bandu.tiji.transfer.runtime.discovery.NsdPeerDiscovery
import com.bandu.tiji.transfer.runtime.discovery.parseManualDiscoveryEndpoint
import com.bandu.tiji.transfer.runtime.discovery.toNearbyDevice
import com.bandu.tiji.transfer.runtime.identity.LocalDeviceIdentity
import com.bandu.tiji.transfer.runtime.transport.BoundTcpServer
import com.bandu.tiji.transfer.runtime.transport.LanTcpTransport
import com.bandu.tiji.transfer.runtime.trust.InMemoryTrustedPeerStore
import com.bandu.tiji.transfer.runtime.trust.TrustedPeerStore
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TransferRuntime(
    private val config: TransferRuntimeConfig = TransferRuntimeConfig(File("build/runtime")),
    private val clock: Clock = SystemClock(),
    private val localIdentityProvider: suspend () -> LocalDeviceIdentity = {
        LocalDeviceIdentity(
            deviceId = "local-device",
            displayName = "Android",
            keyAlias = "memory",
            signingPublicKey = byteArrayOf(1, 2, 3),
            publicKeyFingerprint = "0".repeat(64),
        )
    },
    private val discovery: NsdPeerDiscovery? = null,
    private val trustedPeerStore: TrustedPeerStore = InMemoryTrustedPeerStore(),
    private val pairingPolicy: PairingCodePolicy = PairingCodePolicy(clock),
    private val tcpTransport: LanTcpTransport? = null,
) : TransferRuntimeApi {
    private val fallbackNearbyDevices = MutableStateFlow<List<NearbyDevice>>(emptyList())
    private val transferState = MutableStateFlow<TransferState>(TransferState.Idle)
    private val pairingCounter = AtomicInteger(1)
    private val mutableActivity = MutableStateFlow<RuntimeActivity>(RuntimeActivity.Inactive)
    private var discoveryServer: BoundTcpServer? = null
    private var activeSessionId: String? = null

    override val activity: StateFlow<RuntimeActivity> = mutableActivity.asStateFlow()

    override fun observeNearbyDevices(): Flow<List<NearbyDevice>> =
        discovery?.nearbyDevices ?: fallbackNearbyDevices.asStateFlow()

    override fun observeTrustedDevices(): Flow<List<TrustedDevice>> =
        trustedPeerStore.observeTrustedPeers()

    override fun observeTransferState(): StateFlow<TransferState> = transferState.asStateFlow()

    override suspend fun startDiscovery() {
        val identity = localIdentityProvider()
        val boundServer = tcpTransport?.openServer()
        discoveryServer = boundServer
        discovery?.start(
            localIdentity = identity,
            mode = DiscoveryMode.PAIR,
            port = boundServer?.port ?: DEFAULT_DISCOVERY_PORT,
        )
        fallbackNearbyDevices.value = emptyList()
        transferState.value = TransferState.Discovering
        mutableActivity.value = RuntimeActivity.Discovery(advertising = true, scanning = true)
    }

    override suspend fun stopDiscovery() {
        discovery?.stop()
        discoveryServer?.close()
        discoveryServer = null
        fallbackNearbyDevices.value = emptyList()
        if (transferState.value is TransferState.Discovering) {
            transferState.value = TransferState.Idle
        }
        mutableActivity.value = RuntimeActivity.Inactive
    }

    fun addManualDiscoveryTarget(
        address: String,
        displayName: String = "手动设备",
    ): NearbyDevice {
        check(transferState.value is TransferState.Discovering) {
            "Manual discovery requires an active discovery session"
        }
        val endpoint = parseManualDiscoveryEndpoint(address, DEFAULT_DISCOVERY_PORT)
        val peer = endpoint.toPeer(displayName = displayName)
        discovery?.addManualPeer(peer)
        if (discovery == null) {
            val current = fallbackNearbyDevices.value.filterNot { it.discoveryId == peer.discoveryId }
            fallbackNearbyDevices.value = current + peer.toNearbyDevice()
        }
        return peer.toNearbyDevice()
    }

    override suspend fun createReceiveCode(): PairingCode {
        val issued = pairingPolicy.issue() ?: error("Pairing code is temporarily locked")
        return try {
            val code = issued.value.concatToString()
            transferState.value = TransferState.Pairing(
                peer = NearbyDevice(
                    discoveryId = "pending-${pairingCounter.getAndIncrement()}",
                    displayName = "待配对设备",
                    mode = DiscoveryMode.PAIR,
                ),
                expiresAt = issued.expiresAtEpochMillis,
            )
            code to issued.expiresAtEpochMillis
        } finally {
            issued.value.fill('\u0000')
        }.let { (code, expiresAt) ->
            PairingCode(code = code, expiresAtEpochMillis = expiresAt)
        }
    }

    override suspend fun pair(
        device: NearbyDevice,
        code: String,
    ): PairingResult {
        val validation = pairingPolicy.validate(code.toCharArray())
        return when (validation) {
            PairingCodeValidation.VALID -> {
                trustedPeerStore.upsert(
                    TrustedDevice(
                        deviceId = device.discoveryId,
                        displayName = device.displayName,
                        publicKeyFingerprint = IdentityProof.fingerprint(
                            "runtime-paired-peer:${device.discoveryId}".encodeToByteArray(),
                        ),
                    ),
                )
                transferState.value = TransferState.Idle
                PairingResult.Success
            }
            PairingCodeValidation.EXPIRED ->
                PairingResult.Failure(TransferFailureCode.PAIRING_EXPIRED)
            PairingCodeValidation.INVALID,
            PairingCodeValidation.LOCKED,
            -> PairingResult.Failure(TransferFailureCode.PAIRING_FAILED)
        }
    }

    override suspend fun sendAll(target: TrustedDevice) {
        val trusted = trustedPeerStore.find(target.deviceId)
        if (trusted == null || trusted.publicKeyFingerprint != target.publicKeyFingerprint) {
            transferState.value = TransferState.Failed(
                code = TransferFailureCode.PROTOCOL_ERROR,
                resumable = false,
            )
            return
        }
        val sessionId = "session-${target.deviceId}-${clock.nowEpochMillis()}"
        activeSessionId = sessionId
        mutableActivity.value = RuntimeActivity.Transfer(sessionId, TransferDirection.SEND)
        transferState.value = TransferState.Transferring(
            progress = TransferProgress(
                phase = TransferPhase.TRANSFERRING,
                percentComplete = 0,
                transferredBytes = 0L,
                totalBytes = 1L,
                bytesPerSecond = 0L,
            ),
        )
    }

    override suspend fun acceptTransfer(sessionId: String) {
        if (activeSessionId == null) activeSessionId = sessionId
        transferState.value = when (transferState.value) {
            is TransferState.AwaitingOfferConfirmation,
            is TransferState.Transferring,
            is TransferState.Verifying,
            TransferState.AwaitingFinalConfirmation,
            -> TransferState.AwaitingFinalConfirmation
            else -> TransferState.Completed(
                TransferSummary(
                    sessionId = sessionId,
                    transferredBytes = 0L,
                    durationMillis = 1L,
                ),
            )
        }
        if (transferState.value is TransferState.Completed) {
            mutableActivity.value = RuntimeActivity.Inactive
            activeSessionId = null
        } else {
            mutableActivity.value = RuntimeActivity.Transfer(sessionId, TransferDirection.RECEIVE)
        }
    }

    override suspend fun rejectTransfer(sessionId: String) {
        transferState.value = TransferState.Failed(TransferFailureCode.OFFER_REJECTED, resumable = false)
        mutableActivity.value = RuntimeActivity.Inactive
        activeSessionId = null
    }

    override suspend fun cancelTransfer() {
        transferState.value = TransferState.Failed(TransferFailureCode.CANCELLED, resumable = false)
        mutableActivity.value = RuntimeActivity.Inactive
        activeSessionId = null
    }

    override suspend fun forgetDevice(deviceId: String) {
        trustedPeerStore.remove(deviceId)
    }

    fun injectIncomingOffer(offer: TransferOfferSummary) {
        activeSessionId = offer.sessionId
        transferState.value = TransferState.AwaitingOfferConfirmation(offer)
        mutableActivity.value = RuntimeActivity.Transfer(offer.sessionId, TransferDirection.RECEIVE)
    }

    companion object {
        private const val DEFAULT_DISCOVERY_PORT = 41_241
    }
}
