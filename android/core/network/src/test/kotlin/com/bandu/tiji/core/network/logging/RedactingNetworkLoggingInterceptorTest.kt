package com.bandu.tiji.core.network.logging

import com.bandu.tiji.core.common.logging.AppLogger
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test

class RedactingNetworkLoggingInterceptorTest {
    @Test
    fun `logs only whitelisted metadata for error responses`() {
        val apiKey = "api-key-must-not-be-logged"
        val requestBody = "full-question-and-image-body"
        val errorBody = "provider-error-body-must-not-be-logged"
        val logger = RecordingLogger()

        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(401).setBody(errorBody))
            val client = OkHttpClient.Builder()
                .addInterceptor(RedactingNetworkLoggingInterceptor(logger, incrementingClock()))
                .build()
            val request = Request.Builder()
                .url(server.url("/v1/chat/completions?key=$apiKey"))
                .header("Authorization", "Bearer $apiKey")
                .header("X-Api-Key", apiKey)
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().close()

            val renderedLogs = logger.events.joinToString()
            assertThat(renderedLogs).doesNotContain(apiKey)
            assertThat(renderedLogs).doesNotContain(requestBody)
            assertThat(renderedLogs).doesNotContain(errorBody)
            assertThat(renderedLogs).doesNotContain("Authorization")
            assertThat(renderedLogs).doesNotContain("X-Api-Key")
            assertThat(renderedLogs).doesNotContain("/v1/chat/completions")
            assertThat(renderedLogs).doesNotContain("?key=")
            assertThat(renderedLogs).contains("statusCategory=4xx")
            val serverUrl = server.url("/")
            assertThat(renderedLogs).contains(
                "endpoint=${serverUrl.scheme}://${serverUrl.host}:${serverUrl.port}",
            )
        }
    }

    @Test
    fun `does not log exception messages`() {
        val secret = "secret-from-network-exception"
        val logger = RecordingLogger()
        val client = OkHttpClient.Builder()
            .addInterceptor(RedactingNetworkLoggingInterceptor(logger, incrementingClock()))
            .addInterceptor { throw IOException(secret) }
            .build()
        val request = Request.Builder().url("https://example.com/private?token=$secret").build()

        val thrown = runCatching { client.newCall(request).execute() }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(IOException::class.java)
        val renderedLogs = logger.events.joinToString()
        assertThat(renderedLogs).doesNotContain(secret)
        assertThat(renderedLogs).doesNotContain("token")
        assertThat(renderedLogs).contains("errorType=IOException")
    }

    private fun incrementingClock(): () -> Long {
        var current = 0L
        return {
            current += 1_000_000L
            current
        }
    }
}

private data class LogEvent(
    val level: String,
    val event: String,
    val fields: Map<String, Any?>,
)

private class RecordingLogger : AppLogger {
    val events = mutableListOf<LogEvent>()

    override fun debug(event: String, fields: Map<String, Any?>) {
        events += LogEvent("debug", event, fields)
    }

    override fun info(event: String, fields: Map<String, Any?>) {
        events += LogEvent("info", event, fields)
    }

    override fun warn(event: String, fields: Map<String, Any?>) {
        events += LogEvent("warn", event, fields)
    }

    override fun error(event: String, throwable: Throwable?, fields: Map<String, Any?>) {
        events += LogEvent("error", event, fields)
    }
}
