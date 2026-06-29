package com.bandu.tiji.transfer.runtime

import com.bandu.tiji.core.common.time.Clock
import com.bandu.tiji.domain.transfer.DiscoveryMode
import com.bandu.tiji.domain.transfer.NearbyDevice
import com.bandu.tiji.domain.transfer.PairingResult
import com.bandu.tiji.domain.transfer.TransferFailureCode
import com.bandu.tiji.domain.transfer.TransferState
import com.bandu.tiji.domain.transfer.TrustedDevice
import com.bandu.tiji.transfer.runtime.trust.InMemoryTrustedPeerStore
import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TransferRuntimeApiTest {
    @Test
    fun `runtime defaults match the version one wire contract`() {
        val config = TransferRuntimeConfig(File("build/runtime-test"))

        assertThat(config.serviceType).isEqualTo("_bandu-tiji._tcp.")
        assertThat(config.connectTimeoutMillis).isEqualTo(10_000L)
        assertThat(config.pairingCodeLifetimeMillis).isEqualTo(300_000L)
        assertThat(config.resumeLifetimeMillis).isEqualTo(86_400_000L)
        assertThat(TransferRuntimeConfig.CHUNK_SIZE_BYTES).isEqualTo(1_048_576)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `runtime rejects a different discovery service type`() {
        TransferRuntimeConfig(
            filesDirectory = File("build/runtime-test"),
            serviceType = "_other._tcp.",
        )
    }

    @Test
    fun `pairing code creates trust and forget removes it`() = runTest {
        val runtime = TransferRuntime(clock = MutableClock(1_000L))
        val code = runtime.createReceiveCode()
        val peer = NearbyDevice("peer-a", "旧手机", DiscoveryMode.PAIR)

        val result = runtime.pair(peer, code.code)

        assertThat(result).isEqualTo(PairingResult.Success)
        val trusted = runtime.observeTrustedDevices().first().single()
        assertThat(trusted.deviceId).isEqualTo("peer-a")
        assertThat(trusted.displayName).isEqualTo("旧手机")
        assertThat(trusted.publicKeyFingerprint).hasLength(64)

        runtime.forgetDevice("peer-a")

        assertThat(runtime.observeTrustedDevices().first()).isEmpty()
    }

    @Test
    fun `expired pairing code fails without trusting peer`() = runTest {
        val clock = MutableClock(1_000L)
        val runtime = TransferRuntime(clock = clock)
        val code = runtime.createReceiveCode()
        clock.advance(TransferRuntimeConfig.PAIRING_CODE_LIFETIME_MILLIS + 1)

        val result = runtime.pair(NearbyDevice("peer-a", "旧手机", DiscoveryMode.PAIR), code.code)

        assertThat(result).isEqualTo(PairingResult.Failure(TransferFailureCode.PAIRING_EXPIRED))
        assertThat(runtime.observeTrustedDevices().first()).isEmpty()
    }

    @Test
    fun `five wrong pairing attempts lock code issuance`() = runTest {
        val runtime = TransferRuntime(clock = MutableClock(1_000L))
        runtime.createReceiveCode()
        val peer = NearbyDevice("peer-a", "旧手机", DiscoveryMode.PAIR)

        repeat(5) {
            assertThat(runtime.pair(peer, "000000"))
                .isEqualTo(PairingResult.Failure(TransferFailureCode.PAIRING_FAILED))
        }

        assertThat(runCatching { runtime.createReceiveCode() }.exceptionOrNull())
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `sendAll rejects target when trusted identity fingerprint changed`() = runTest {
        val store = InMemoryTrustedPeerStore(
            listOf(TrustedDevice("peer-a", "旧手机", "a".repeat(64))),
        )
        val runtime = TransferRuntime(clock = MutableClock(1_000L), trustedPeerStore = store)

        runtime.sendAll(TrustedDevice("peer-a", "旧手机", "b".repeat(64)))

        assertThat(runtime.observeTransferState().value)
            .isEqualTo(TransferState.Failed(TransferFailureCode.PROTOCOL_ERROR, resumable = false))
    }

    @Test
    fun `sendAll starts observable transfer for trusted peer`() = runTest {
        val trusted = TrustedDevice("peer-a", "旧手机", "a".repeat(64))
        val store = InMemoryTrustedPeerStore(listOf(trusted))
        val runtime = TransferRuntime(clock = MutableClock(1_000L), trustedPeerStore = store)

        runtime.sendAll(trusted)

        assertThat(runtime.observeTransferState().value)
            .isInstanceOf(TransferState.Transferring::class.java)
        assertThat(runtime.activity.value)
            .isEqualTo(RuntimeActivity.Transfer("session-peer-a-1000", TransferDirection.SEND))
    }

    @Test
    fun `manual discovery target is only accepted during active discovery`() = runTest {
        val runtime = TransferRuntime(clock = MutableClock(1_000L))

        assertThat(runCatching { runtime.addManualDiscoveryTarget("10.0.2.2") }.exceptionOrNull())
            .isInstanceOf(IllegalStateException::class.java)

        runtime.startDiscovery()
        val device = runtime.addManualDiscoveryTarget("10.0.2.2:41241", displayName = "目标设备")

        assertThat(device.displayName).isEqualTo("目标设备")
        assertThat(runtime.observeNearbyDevices().first()).containsExactly(device)

        runtime.stopDiscovery()

        assertThat(runtime.observeNearbyDevices().first()).isEmpty()
    }
}

private class MutableClock(private var now: Long) : Clock {
    override fun nowEpochMillis(): Long = now

    fun advance(millis: Long) {
        now += millis
    }
}
