package com.bandu.tiji.core.network.endpoint

import com.bandu.tiji.core.network.tls.SystemTlsClientFactory
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.Test

class DefaultEndpointPolicyTlsTest {
    @Test
    fun `https is allowed before normal platform certificate validation`() {
        val decision = runImmediateSuspend {
            DefaultEndpointPolicy().validate(
                url = "https://example.com/v1".toHttpUrl(),
                allowPrivateCleartext = false,
            )
        }

        assertThat(decision).isEqualTo(EndpointDecision.Allowed)
    }

    @Test
    fun `system tls client rejects an untrusted certificate`() {
        val certificate = HeldCertificate.Builder()
            .commonName("localhost")
            .addSubjectAlternativeName("localhost")
            .build()
        val serverCertificates = HandshakeCertificates.Builder()
            .heldCertificate(certificate)
            .build()

        MockWebServer().use { server ->
            server.useHttps(
                sslSocketFactory = serverCertificates.sslSocketFactory(),
                tunnelProxy = false,
            )
            server.enqueue(MockResponse().setBody("must not be trusted"))
            server.start()

            val failure = runCatching {
                SystemTlsClientFactory.create()
                    .newCall(Request.Builder().url(server.url("/")).build())
                    .execute()
                    .use { }
            }.exceptionOrNull()

            assertThat(failure).isInstanceOf(IOException::class.java)
            assertThat(server.requestCount).isEqualTo(0)
        }
    }
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
