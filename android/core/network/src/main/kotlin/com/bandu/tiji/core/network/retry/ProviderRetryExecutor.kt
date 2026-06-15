package com.bandu.tiji.core.network.retry

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay

sealed interface ProviderRetryDecision {
    data object DoNotRetry : ProviderRetryDecision

    data class RetryAfter(
        val delayMillis: Long,
    ) : ProviderRetryDecision {
        init {
            require(delayMillis >= 0L) { "delayMillis must not be negative" }
        }
    }
}

class ProviderRetryExecutor(
    private val delayAction: suspend (Long) -> Unit = { delay(it) },
) {
    suspend fun <T> execute(
        decide: (failure: Throwable, failedAttempt: Int) -> ProviderRetryDecision,
        block: suspend (attempt: Int) -> T,
    ): T {
        var attempt = 0
        while (true) {
            try {
                return block(attempt)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                if (attempt >= MAX_RETRIES) throw failure
                when (val decision = decide(failure, attempt)) {
                    ProviderRetryDecision.DoNotRetry -> throw failure
                    is ProviderRetryDecision.RetryAfter -> {
                        delayAction(decision.delayMillis)
                        attempt += 1
                    }
                }
            }
        }
    }

    private companion object {
        const val MAX_RETRIES = 1
    }
}
