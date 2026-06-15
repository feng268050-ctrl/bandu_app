package com.bandu.tiji.ai.gemini

import com.bandu.tiji.ai.api.error.AiError
import com.bandu.tiji.core.network.endpoint.EndpointRejectedException
import com.bandu.tiji.core.network.endpoint.EndpointRejectionReason
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import java.net.SocketTimeoutException
import org.junit.Test

class GeminiErrorMapperTest {
    @Test
    fun `maps HTTP authentication rate limit timeout and server failures`() {
        assertThat(GeminiErrorMapper.map(GeminiHttpException(401)))
            .isEqualTo(AiError.Authentication)
        assertThat(GeminiErrorMapper.map(GeminiHttpException(403)))
            .isEqualTo(AiError.Authentication)
        assertThat(GeminiErrorMapper.map(GeminiHttpException(429)))
            .isEqualTo(AiError.RateLimited)
        assertThat(GeminiErrorMapper.map(GeminiHttpException(408)))
            .isEqualTo(AiError.Timeout)
        assertThat(GeminiErrorMapper.map(GeminiHttpException(504)))
            .isEqualTo(AiError.Timeout)
        assertThat(GeminiErrorMapper.map(GeminiHttpException(503)))
            .isEqualTo(AiError.NetworkUnavailable)
    }

    @Test
    fun `maps network endpoint and invalid response failures`() {
        assertThat(GeminiErrorMapper.map(SocketTimeoutException()))
            .isEqualTo(AiError.Timeout)
        assertThat(GeminiErrorMapper.map(IOException()))
            .isEqualTo(AiError.NetworkUnavailable)
        assertThat(
            GeminiErrorMapper.map(
                EndpointRejectedException(
                    EndpointRejectionReason.DNS_CONTAINS_PUBLIC_ADDRESS,
                ),
            ),
        ).isEqualTo(AiError.EndpointRejected("dns_contains_public_address"))
        assertThat(GeminiErrorMapper.map(GeminiHttpException(400)))
            .isEqualTo(AiError.InvalidResponse("gemini.http_400"))
        assertThat(
            GeminiErrorMapper.map(AiError.InvalidResponse("gemini.invalid_json")),
        ).isEqualTo(AiError.InvalidResponse("gemini.invalid_json"))
    }

    @Test
    fun `validation codes never expose exception messages`() {
        val secret = "provider-body-secret"

        val code = GeminiErrorMapper.validationCode(IOException(secret))

        assertThat(code).isEqualTo("network_error")
        assertThat(code).doesNotContain(secret)
    }
}
