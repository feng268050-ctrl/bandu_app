package com.bandu.tiji.core.network

import com.google.common.truth.Truth.assertThat
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test

class HttpEngineTest {
    @Test
    fun `executes request through injected call factory`() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("ok"))
            val engine = HttpEngine(OkHttpClient())
            val request = Request.Builder().url(server.url("/health")).build()

            engine.newCall(request).execute().use { response ->
                assertThat(response.code).isEqualTo(200)
                assertThat(response.body.string()).isEqualTo("ok")
            }

            assertThat(server.takeRequest().path).isEqualTo("/health")
        }
    }
}
