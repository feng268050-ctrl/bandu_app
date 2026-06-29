package com.bandu.tiji.transfer.runtime.discovery

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bandu.tiji.domain.transfer.DiscoveryMode
import com.bandu.tiji.transfer.runtime.identity.LocalDeviceIdentity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NsdPeerDiscoveryTwoDeviceInstrumentedTest {
    @Test
    fun twoDeviceNsdDiscoveryWhenRoleArgumentsAreProvided() = runBlocking {
        val arguments = InstrumentationRegistry.getArguments()
        val role = arguments.getString("role") ?: return@runBlocking
        val runId = requireNotNull(arguments.getString("runId")) { "runId argument is required" }
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val discovery = NsdPeerDiscovery(AndroidNsdDiscoveryBackend(context))
        val displayName = when (role) {
            ROLE_ADVERTISER -> "Bandu advertiser $runId"
            ROLE_SCANNER -> "Bandu scanner $runId"
            else -> error("Unsupported role: $role")
        }

        discovery.start(
            localIdentity = localIdentity(
                deviceId = "$role-$runId",
                displayName = displayName,
            ),
            mode = DiscoveryMode.PAIR,
            port = 41241,
        )
        try {
            when (role) {
                ROLE_ADVERTISER -> delay(45_000)
                ROLE_SCANNER -> {
                    val deadline = System.currentTimeMillis() + 45_000
                    while (
                        System.currentTimeMillis() < deadline &&
                        discovery.nearbyDevices.value.none { it.displayName == "Bandu advertiser $runId" }
                    ) {
                        delay(500)
                    }
                    assertThat(discovery.nearbyDevices.value.map { it.displayName })
                        .contains("Bandu advertiser $runId")
                }
            }
        } finally {
            discovery.stop()
        }
    }

    private fun localIdentity(
        deviceId: String,
        displayName: String,
    ): LocalDeviceIdentity =
        LocalDeviceIdentity(
            deviceId = deviceId,
            displayName = displayName,
            keyAlias = "alias",
            signingPublicKey = byteArrayOf(1, 2, 3),
            publicKeyFingerprint = "b".repeat(64),
        )

    companion object {
        private const val ROLE_ADVERTISER = "advertiser"
        private const val ROLE_SCANNER = "scanner"
    }
}
