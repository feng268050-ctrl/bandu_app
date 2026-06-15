package com.bandu.tiji.ai.api.model

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AiStreamEventTest {
    @Test
    fun `stream events preserve order`() = runTest {
        val events = kotlinx.coroutines.flow.flow {
            emit(AiStreamEvent.Delta("你"))
            emit(AiStreamEvent.Delta("好"))
            emit(AiStreamEvent.Usage(inputTokens = 10, outputTokens = 2))
            emit(AiStreamEvent.Completed)
        }.toList()

        assertThat(events[0]).isEqualTo(AiStreamEvent.Delta("你"))
        assertThat(events[1]).isEqualTo(AiStreamEvent.Delta("好"))
        assertThat(events[2]).isEqualTo(AiStreamEvent.Usage(10, 2))
        assertThat(events[3]).isEqualTo(AiStreamEvent.Completed)
    }
}
