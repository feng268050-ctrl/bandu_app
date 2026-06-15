package com.bandu.tiji.ai.openai

import com.bandu.tiji.ai.api.error.AiError
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import java.net.SocketTimeoutException
import org.junit.Test

class OpenAiErrorMapperTest {
    @Test
    fun `maps HTTP statuses to common AI errors`() {
        assertThat(OpenAiErrorMapper.map(OpenAiHttpException(401)))
            .isEqualTo(AiError.Authentication)
        assertThat(OpenAiErrorMapper.map(OpenAiHttpException(403)))
            .isEqualTo(AiError.Authentication)
        assertThat(OpenAiErrorMapper.map(OpenAiHttpException(429)))
            .isEqualTo(AiError.RateLimited)
        assertThat(OpenAiErrorMapper.map(OpenAiHttpException(408)))
            .isEqualTo(AiError.Timeout)
        assertThat(OpenAiErrorMapper.map(OpenAiHttpException(504)))
            .isEqualTo(AiError.Timeout)
        assertThat(OpenAiErrorMapper.map(OpenAiHttpException(503)))
            .isEqualTo(AiError.NetworkUnavailable)
    }

    @Test
    fun `maps common nested and flat compatible JSON shapes`() {
        assertThat(
            OpenAiErrorMapper.map(
                OpenAiHttpException(
                    400,
                    """{"error":{"message":"bad key","type":"invalid_request_error","code":"invalid_api_key"}}""",
                ),
            ),
        ).isEqualTo(AiError.Authentication)
        assertThat(
            OpenAiErrorMapper.map(
                OpenAiHttpException(
                    400,
                    """{"error":"quota_exceeded"}""",
                ),
            ),
        ).isEqualTo(AiError.RateLimited)
        assertThat(
            OpenAiErrorMapper.map(
                OpenAiHttpException(
                    400,
                    """{"message":"upstream timed out","type":"request_timeout"}""",
                ),
            ),
        ).isEqualTo(AiError.Timeout)
    }

    @Test
    fun `string and empty response bodies become stable diagnostics`() {
        val secret = "provider-secret-message"

        val stringError = OpenAiErrorMapper.map(OpenAiHttpException(400, secret))
        val emptyError = OpenAiErrorMapper.map(OpenAiHttpException(422, ""))

        assertThat(stringError).isEqualTo(AiError.InvalidResponse("openai.http_400"))
        assertThat(emptyError).isEqualTo(AiError.InvalidResponse("openai.http_422"))
        assertThat(stringError.toString()).doesNotContain(secret)
    }

    @Test
    fun `maps network failures and keeps validation codes generic`() {
        assertThat(OpenAiErrorMapper.map(SocketTimeoutException()))
            .isEqualTo(AiError.Timeout)
        assertThat(OpenAiErrorMapper.map(IOException()))
            .isEqualTo(AiError.NetworkUnavailable)

        val code = OpenAiErrorMapper.validationCode(IOException("private host detail"))

        assertThat(code).isEqualTo("network_error")
        assertThat(code).doesNotContain("private host detail")
    }
}
