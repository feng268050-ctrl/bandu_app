package com.bandu.tiji.core.network.endpoint

import com.google.common.truth.Truth.assertThat
import java.net.InetAddress
import java.net.UnknownHostException
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Test

class DefaultEndpointPolicyDnsTest {
    @Test
    fun `allows cleartext domain only when every answer is private`() = runTest {
        val policy = DefaultEndpointPolicy(
            hostResolver = HostResolver {
                listOf(
                    InetAddress.getByName("192.168.1.10"),
                    InetAddress.getByName("fd00::10"),
                )
            },
        )

        assertThat(
            policy.validate("http://device.local/v1".toHttpUrl(), true),
        ).isEqualTo(EndpointDecision.Allowed)
    }

    @Test
    fun `rejects mixed DNS answers containing a public address`() = runTest {
        val policy = DefaultEndpointPolicy(
            hostResolver = HostResolver {
                listOf(
                    InetAddress.getByName("10.0.0.4"),
                    InetAddress.getByName("8.8.8.8"),
                )
            },
        )

        assertThat(
            policy.validate("http://mixed.local/v1".toHttpUrl(), true),
        ).isEqualTo(
            EndpointDecision.Rejected(
                EndpointRejectionReason.DNS_CONTAINS_PUBLIC_ADDRESS,
            ),
        )
    }

    @Test
    fun `maps empty and failed DNS lookup separately from public answers`() = runTest {
        val emptyPolicy = DefaultEndpointPolicy(HostResolver { emptyList() })
        val failedPolicy = DefaultEndpointPolicy(
            HostResolver { throw UnknownHostException("missing") },
        )
        val url = "http://missing.local/v1".toHttpUrl()
        val expected = EndpointDecision.Rejected(
            EndpointRejectionReason.DNS_RESOLUTION_FAILED,
        )

        assertThat(emptyPolicy.validate(url, true)).isEqualTo(expected)
        assertThat(failedPolicy.validate(url, true)).isEqualTo(expected)
    }

    @Test
    fun `confirmation is checked before DNS resolution`() = runTest {
        var resolutionCount = 0
        val policy = DefaultEndpointPolicy(
            HostResolver {
                resolutionCount += 1
                listOf(InetAddress.getByName("10.0.0.1"))
            },
        )

        assertThat(
            policy.validate("http://private.local/v1".toHttpUrl(), false),
        ).isEqualTo(
            EndpointDecision.Rejected(
                EndpointRejectionReason.CLEARTEXT_NOT_CONFIRMED,
            ),
        )
        assertThat(resolutionCount).isEqualTo(0)
    }
}
