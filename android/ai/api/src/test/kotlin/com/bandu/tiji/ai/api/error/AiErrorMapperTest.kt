package com.bandu.tiji.ai.api.error

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class AiErrorMapperTest {
    @Test
    fun `maps http status to ai errors`() {
        assertThat(AiErrorMapper.fromHttpStatus(401)).isEqualTo(AiError.Authentication)
        assertThat(AiErrorMapper.fromHttpStatus(403)).isEqualTo(AiError.Authentication)
        assertThat(AiErrorMapper.fromHttpStatus(429)).isEqualTo(AiError.RateLimited)
        assertThat(AiErrorMapper.fromHttpStatus(503)).isEqualTo(AiError.NetworkUnavailable)
        assertThat(AiErrorMapper.fromHttpStatus(404)).isNull()
    }

    @Test
    fun `maps throwables to ai errors`() {
        assertThat(AiErrorMapper.fromThrowable(SocketTimeoutException())).isEqualTo(AiError.Timeout)
        assertThat(AiErrorMapper.fromThrowable(UnknownHostException())).isEqualTo(AiError.NetworkUnavailable)
        assertThat(AiErrorMapper.fromThrowable(IOException())).isEqualTo(AiError.NetworkUnavailable)
        assertThat(AiErrorMapper.fromThrowable(AiError.Cancelled)).isEqualTo(AiError.Cancelled)
    }
}
