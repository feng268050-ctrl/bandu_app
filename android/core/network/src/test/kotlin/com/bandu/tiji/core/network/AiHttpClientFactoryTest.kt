package com.bandu.tiji.core.network

import com.bandu.tiji.core.network.endpoint.DefaultEndpointPolicy
import com.google.common.truth.Truth.assertThat
import java.io.InterruptedIOException
import java.time.Duration
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Test

class AiHttpClientFactoryTest {
    @Test
    fun `operation profiles use the specified total timeouts`() {
        val expectedMillis = mapOf(
            AiHttpOperation.CONFIGURATION_VALIDATION to 20_000,
            AiHttpOperation.IMAGE_ANALYSIS to 180_000,
            AiHttpOperation.TUTOR_STREAM to 600_000,
            AiHttpOperation.EXERCISE to 120_000,
        )

        expectedMillis.forEach { (operation, timeoutMillis) ->
            val client = AiHttpClientFactory.create(
                operation = operation,
                endpointPolicy = DefaultEndpointPolicy(),
                allowPrivateCleartext = { true },
            )

            assertThat(client.connectTimeoutMillis).isEqualTo(10_000)
            assertThat(client.readTimeoutMillis).isEqualTo(60_000)
            assertThat(client.writeTimeoutMillis).isEqualTo(60_000)
            assertThat(client.callTimeoutMillis).isEqualTo(timeoutMillis)
            assertThat(client.retryOnConnectionFailure).isFalse()
        }
    }

    @Test
    fun `failed post is not retried automatically`() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START),
            )
            server.enqueue(MockResponse().setBody("must not be reached"))
            val client = AiHttpClientFactory.create(
                operation = AiHttpOperation.IMAGE_ANALYSIS,
                endpointPolicy = DefaultEndpointPolicy(),
                allowPrivateCleartext = { true },
            )
            val request = Request.Builder()
                .url(server.url("/analyze"))
                .post("""{"question":"sensitive"}""".toRequestBody("application/json".toMediaType()))
                .build()

            val failure = runCatching { client.newCall(request).execute() }.exceptionOrNull()

            assertThat(failure).isInstanceOf(java.io.IOException::class.java)
            assertThat(server.requestCount).isEqualTo(1)
        }
    }

    @Test
    fun `call timeout cancels a delayed response`() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setBody("late")
                    .setBodyDelay(1, TimeUnit.SECONDS),
            )
            val client = AiHttpClientFactory.create(
                timeouts = AiHttpTimeouts(
                    connect = Duration.ofSeconds(1),
                    read = Duration.ofSeconds(2),
                    write = Duration.ofSeconds(2),
                    call = Duration.ofMillis(100),
                ),
                endpointPolicy = DefaultEndpointPolicy(),
                allowPrivateCleartext = { true },
            )

            val failure = runCatching {
                client.newCall(Request.Builder().url(server.url("/slow")).build())
                    .execute()
                    .use { it.body.string() }
            }.exceptionOrNull()

            assertThat(failure).isInstanceOf(InterruptedIOException::class.java)
        }
    }

    @Test
    fun `timeouts must be positive`() {
        assertThat(
            runCatching {
                AiHttpTimeouts(call = Duration.ZERO)
            }.exceptionOrNull(),
        ).isInstanceOf(IllegalArgumentException::class.java)
    }
}
