package com.bandu.tiji.ai.api.retry

import com.bandu.tiji.ai.api.error.AiError
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RetryPolicyTest {
    private val policy = RetryPolicy(maxAttempts = 1)

    @Test
    fun `network and server errors retry once`() {
        listOf(
            AiError.NetworkUnavailable,
            AiError.Timeout,
        ).forEach { error ->
            val decision = policy.decide(
                RetryContext(
                    operation = AiOperationType.ANALYZE_IMAGE,
                    attempt = 0,
                    error = error,
                ),
            )
            assertThat(decision).isInstanceOf(RetryDecision.RetryAfter::class.java)
        }

        val serverError = policy.decide(
            RetryContext(
                operation = AiOperationType.GENERATE_EXERCISE,
                attempt = 0,
                error = AiError.NetworkUnavailable,
                httpStatus = 503,
            ),
        )
        assertThat(serverError).isInstanceOf(RetryDecision.RetryAfter::class.java)
    }

    @Test
    fun `cancelled auth and invalid response do not retry`() {
        val cases = listOf(
            AiError.Cancelled,
            AiError.Authentication,
            AiError.InvalidResponse("analysis.missing_tag"),
            AiError.ConfigurationRequired,
            AiError.EndpointRejected("http rejected"),
        )
        cases.forEach { error ->
            val decision = policy.decide(
                RetryContext(
                    operation = AiOperationType.ANALYZE_IMAGE,
                    attempt = 0,
                    error = error,
                ),
            )
            assertThat(decision).isEqualTo(RetryDecision.DoNotRetry)
        }
    }

    @Test
    fun `configuration validation never retries`() {
        val decision = policy.decide(
            RetryContext(
                operation = AiOperationType.VALIDATE_CONFIGURATION,
                attempt = 0,
                error = AiError.NetworkUnavailable,
            ),
        )
        assertThat(decision).isEqualTo(RetryDecision.DoNotRetry)
    }

    @Test
    fun `stream tutor does not retry after delta received`() {
        val decision = policy.decide(
            RetryContext(
                operation = AiOperationType.STREAM_TUTOR_FIRST_BYTE,
                attempt = 0,
                error = AiError.Timeout,
                receivedStreamDelta = true,
            ),
        )
        assertThat(decision).isEqualTo(RetryDecision.DoNotRetry)
    }

    @Test
    fun `second attempt never retries`() {
        val decision = policy.decide(
            RetryContext(
                operation = AiOperationType.ANALYZE_IMAGE,
                attempt = 1,
                error = AiError.NetworkUnavailable,
            ),
        )
        assertThat(decision).isEqualTo(RetryDecision.DoNotRetry)
    }
}
