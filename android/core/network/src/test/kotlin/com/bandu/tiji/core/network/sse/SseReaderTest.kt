package com.bandu.tiji.core.network.sse

import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.Test
import kotlinx.coroutines.test.runTest

class SseReaderTest {
    @Test
    fun `decodes UTF-8 characters split across network buffers`() {
        val body = """
            data: {"delta":"你"}

            data: {"delta":"好"}

            data: [DONE]

        """.trimIndent()

        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .addHeader("Content-Type", "text/event-stream; charset=utf-8")
                    .setBody(body)
                    .throttleBody(1, 1, TimeUnit.MILLISECONDS),
            )

            val events = OkHttpClient()
                .newCall(Request.Builder().url(server.url("/stream")).build())
                .execute()
                .use { response ->
                    SseReader().read(response.body.source())
                }

            assertThat(events).containsExactly(
                SseEvent.Data("""{"delta":"你"}"""),
                SseEvent.Data("""{"delta":"好"}"""),
                SseEvent.Done,
            ).inOrder()
        }
    }

    @Test
    fun `joins multiple data lines and preserves event metadata`() {
        val source = Buffer().writeUtf8(
            """
            : comment
            id: message-1
            event: tutor
            data: first
            data: second

            """.trimIndent() + "\n",
        )

        assertThat(SseReader().read(source)).containsExactly(
            SseEvent.Data(
                data = "first\nsecond",
                event = "tutor",
                id = "message-1",
            ),
        )
    }

    @Test
    fun `dispatches final event at EOF and ignores empty blocks`() {
        val source = Buffer().writeUtf8("\n\ndata: final")

        assertThat(SseReader().read(source)).containsExactly(
            SseEvent.Data("final"),
        )
    }

    @Test
    fun `readEach delivers events in order and stops at done`() = runTest {
        val source = Buffer().writeUtf8(
            """
            data: first

            data: second

            data: [DONE]

            data: ignored

            """.trimIndent() + "\n",
        )
        val events = mutableListOf<SseEvent>()

        SseReader().readEach(source) { event ->
            events += event
        }

        assertThat(events).containsExactly(
            SseEvent.Data("first"),
            SseEvent.Data("second"),
            SseEvent.Done,
        ).inOrder()
    }
}
