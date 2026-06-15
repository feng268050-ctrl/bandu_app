package com.bandu.tiji.core.network.endpoint

import com.google.common.truth.Truth.assertThat
import java.net.InetAddress
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.Test

class EndpointPolicyInterceptorTest {
    @Test
    fun `public cleartext redirect is rejected before a follow up request`() {
        val policy = DefaultEndpointPolicy(
            HostResolver { listOf(InetAddress.getByName("8.8.8.8")) },
        )
        val client = OkHttpClient.Builder()
            .enforceEndpointPolicy(policy) { true }
            .build()

        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setResponseCode(302)
                    .addHeader("Location", "http://public.example/v1"),
            )

            val failure = runCatching {
                client.newCall(Request.Builder().url(server.url("/start")).build())
                    .execute()
                    .use { }
            }.exceptionOrNull()

            assertThat(failure).isInstanceOf(EndpointRejectedException::class.java)
            assertThat((failure as EndpointRejectedException).rejectionReason)
                .isEqualTo(EndpointRejectionReason.DNS_CONTAINS_PUBLIC_ADDRESS)
            assertThat(server.requestCount).isEqualTo(1)
        }
    }

    @Test
    fun `https downgrade is allowed only for a confirmed private target`() {
        val certificate = HeldCertificate.Builder()
            .commonName("localhost")
            .addSubjectAlternativeName("localhost")
            .build()
        val serverCertificates = HandshakeCertificates.Builder()
            .heldCertificate(certificate)
            .build()
        val clientCertificates = HandshakeCertificates.Builder()
            .addTrustedCertificate(certificate.certificate)
            .build()

        MockWebServer().use { redirectTarget ->
            redirectTarget.enqueue(MockResponse().setBody("private result"))
            MockWebServer().use { origin ->
                origin.useHttps(
                    sslSocketFactory = serverCertificates.sslSocketFactory(),
                    tunnelProxy = false,
                )
                origin.enqueue(
                    MockResponse()
                        .setResponseCode(302)
                        .addHeader("Location", redirectTarget.url("/private")),
                )
                val policy = RecordingPolicy(DefaultEndpointPolicy())
                val client = OkHttpClient.Builder()
                    .sslSocketFactory(
                        clientCertificates.sslSocketFactory(),
                        clientCertificates.trustManager,
                    )
                    .enforceEndpointPolicy(policy) { true }
                    .build()

                client.newCall(Request.Builder().url(origin.url("/start")).build())
                    .execute()
                    .use { response ->
                        assertThat(response.body.string()).isEqualTo("private result")
                    }

                assertThat(origin.requestCount).isEqualTo(1)
                assertThat(redirectTarget.requestCount).isEqualTo(1)
                assertThat(policy.validatedUrls.count { it == redirectTarget.url("/private") })
                    .isAtLeast(2)
            }
        }
    }

    @Test
    fun `each redirect target is revalidated`() {
        val policy = RecordingPolicy(DefaultEndpointPolicy())
        val client = OkHttpClient.Builder()
            .enforceEndpointPolicy(policy) { true }
            .build()

        MockWebServer().use { finalServer ->
            finalServer.enqueue(MockResponse().setBody("done"))
            MockWebServer().use { middleServer ->
                middleServer.enqueue(
                    MockResponse()
                        .setResponseCode(302)
                        .addHeader("Location", finalServer.url("/final")),
                )
                MockWebServer().use { firstServer ->
                    firstServer.enqueue(
                        MockResponse()
                            .setResponseCode(302)
                            .addHeader("Location", middleServer.url("/middle")),
                    )

                    client.newCall(Request.Builder().url(firstServer.url("/first")).build())
                        .execute()
                        .close()

                    assertThat(policy.validatedUrls).containsAtLeast(
                        firstServer.url("/first"),
                        middleServer.url("/middle"),
                        finalServer.url("/final"),
                    )
                }
            }
        }
    }
}

private class RecordingPolicy(
    private val delegate: EndpointPolicy,
) : EndpointPolicy {
    val validatedUrls = mutableListOf<okhttp3.HttpUrl>()

    override suspend fun validate(
        url: okhttp3.HttpUrl,
        allowPrivateCleartext: Boolean,
    ): EndpointDecision {
        validatedUrls += url
        return delegate.validate(url, allowPrivateCleartext)
    }
}
