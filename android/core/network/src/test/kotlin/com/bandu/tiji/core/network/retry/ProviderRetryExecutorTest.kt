package com.bandu.tiji.core.network.retry

import com.google.common.truth.Truth.assertThat
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test

class ProviderRetryExecutorTest {
    @Test
    fun `provider can approve exactly one retry`() = runTest {
        val delays = mutableListOf<Long>()
        val attempts = mutableListOf<Int>()
        val executor = ProviderRetryExecutor { delays += it }

        val result = executor.execute(
            decide = { _, _ -> ProviderRetryDecision.RetryAfter(25L) },
            block = { attempt ->
                attempts += attempt
                if (attempt == 0) throw IOException("temporary")
                "success"
            },
        )

        assertThat(result).isEqualTo("success")
        assertThat(attempts).containsExactly(0, 1).inOrder()
        assertThat(delays).containsExactly(25L)
    }

    @Test
    fun `provider can reject retry`() = runTest {
        var calls = 0
        val executor = ProviderRetryExecutor()

        val failure = runCatching {
            executor.execute(
                decide = { _, _ -> ProviderRetryDecision.DoNotRetry },
                block = {
                    calls += 1
                    throw IOException("permanent")
                },
            )
        }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IOException::class.java)
        assertThat(calls).isEqualTo(1)
    }

    @Test
    fun `second failure is returned without another decision`() = runTest {
        var calls = 0
        var decisions = 0
        val executor = ProviderRetryExecutor { }

        val failure = runCatching {
            executor.execute(
                decide = { _, _ ->
                    decisions += 1
                    ProviderRetryDecision.RetryAfter(0L)
                },
                block = {
                    calls += 1
                    throw IOException("failure-$calls")
                },
            )
        }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IOException::class.java)
        assertThat(failure).hasMessageThat().isEqualTo("failure-2")
        assertThat(calls).isEqualTo(2)
        assertThat(decisions).isEqualTo(1)
    }

    @Test
    fun `cancellation is never offered for retry`() {
        var decisions = 0
        val executor = ProviderRetryExecutor()

        assertThrows(CancellationException::class.java) {
            kotlinx.coroutines.runBlocking {
                executor.execute(
                    decide = { _, _ ->
                        decisions += 1
                        ProviderRetryDecision.RetryAfter(0L)
                    },
                    block = { throw CancellationException("cancelled") },
                )
            }
        }
        assertThat(decisions).isEqualTo(0)
    }
}
