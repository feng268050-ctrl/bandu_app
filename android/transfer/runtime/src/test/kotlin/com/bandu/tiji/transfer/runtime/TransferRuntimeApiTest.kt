package com.bandu.tiji.transfer.runtime

import com.google.common.truth.Truth.assertThat
import java.io.File
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
}
