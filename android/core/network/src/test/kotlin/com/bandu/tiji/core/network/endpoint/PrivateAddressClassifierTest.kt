package com.bandu.tiji.core.network.endpoint

import com.google.common.truth.Truth.assertThat
import java.net.InetAddress
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Test

class PrivateAddressClassifierTest {
    @Test
    fun `allows localhost loopback and private IPv4 ranges`() {
        assertThat(PrivateAddressClassifier.isLocalhost("LOCALHOST")).isTrue()
        listOf(
            "127.0.0.1",
            "127.255.255.254",
            "10.0.0.1",
            "10.255.255.254",
            "172.16.0.1",
            "172.31.255.254",
            "192.168.0.1",
            "192.168.255.254",
        ).forEach { address ->
            assertThat(
                PrivateAddressClassifier.isAllowed(InetAddress.getByName(address)),
            ).isTrue()
        }
    }

    @Test
    fun `allows IPv6 loopback link local and unique local ranges`() {
        listOf(
            "::1",
            "fe80::1",
            "febf::ffff",
            "fc00::1",
            "fdff::1",
        ).forEach { address ->
            assertThat(
                PrivateAddressClassifier.isAllowed(InetAddress.getByName(address)),
            ).isTrue()
        }
    }

    @Test
    fun `rejects public unspecified multicast and adjacent ranges`() {
        listOf(
            "0.0.0.0",
            "8.8.8.8",
            "172.15.255.255",
            "172.32.0.1",
            "192.167.1.1",
            "224.0.0.1",
            "::",
            "2001:4860:4860::8888",
            "ff02::1",
        ).forEach { address ->
            assertThat(
                PrivateAddressClassifier.isAllowed(InetAddress.getByName(address)),
            ).isFalse()
        }
    }

    @Test
    fun `cleartext policy accepts only confirmed local literals`() {
        val policy = DefaultEndpointPolicy()

        assertThat(policy.validateNow("http://192.168.1.2/v1", true))
            .isEqualTo(EndpointDecision.Allowed)
        assertThat(policy.validateNow("http://[fd00::1]/v1", true))
            .isEqualTo(EndpointDecision.Allowed)
        assertThat(policy.validateNow("http://8.8.8.8/v1", true))
            .isEqualTo(
                EndpointDecision.Rejected(
                    EndpointRejectionReason.CLEARTEXT_HOST_NOT_PRIVATE,
                ),
            )
        assertThat(policy.validateNow("http://localhost/v1", false))
            .isEqualTo(
                EndpointDecision.Rejected(
                    EndpointRejectionReason.CLEARTEXT_NOT_CONFIRMED,
                ),
            )
    }
}

private fun DefaultEndpointPolicy.validateNow(
    url: String,
    allowPrivateCleartext: Boolean,
): EndpointDecision = runImmediateSuspend {
    validate(url.toHttpUrl(), allowPrivateCleartext)
}

private fun <T> runImmediateSuspend(block: suspend () -> T): T {
    var outcome: Result<T>? = null
    block.startCoroutine(
        object : Continuation<T> {
            override val context = EmptyCoroutineContext

            override fun resumeWith(result: Result<T>) {
                outcome = result
            }
        },
    )
    return checkNotNull(outcome).getOrThrow()
}
