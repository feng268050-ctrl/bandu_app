package com.bandu.tiji.core.network.endpoint

import com.google.common.truth.Truth.assertThat
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

class EndpointPolicyContractTest {
    @Test
    fun `policy receives the endpoint and cleartext confirmation`() {
        val expectedUrl = "http://192.168.1.10/v1".toHttpUrl()
        var capturedUrl = "https://example.invalid".toHttpUrl()
        var capturedConfirmation = false
        val policy = EndpointPolicy { url, allowPrivateCleartext ->
            capturedUrl = url
            capturedConfirmation = allowPrivateCleartext
            EndpointDecision.Allowed
        }

        val decision = runImmediateSuspend {
            policy.validate(
                url = expectedUrl,
                allowPrivateCleartext = true,
            )
        }

        assertThat(decision).isEqualTo(EndpointDecision.Allowed)
        assertThat(capturedUrl).isEqualTo(expectedUrl)
        assertThat(capturedConfirmation).isTrue()
    }

    @Test
    fun `rejected decision preserves a machine readable reason`() {
        EndpointRejectionReason.entries.forEach { reason ->
            val decision: EndpointDecision = EndpointDecision.Rejected(reason)

            assertThat((decision as EndpointDecision.Rejected).reason).isEqualTo(reason)
        }
    }

    @Test
    fun `rejection reasons cover each endpoint policy failure category`() {
        assertThat(EndpointRejectionReason.entries).containsExactly(
            EndpointRejectionReason.UNSUPPORTED_SCHEME,
            EndpointRejectionReason.CLEARTEXT_NOT_CONFIRMED,
            EndpointRejectionReason.CLEARTEXT_HOST_NOT_PRIVATE,
            EndpointRejectionReason.DNS_RESOLUTION_FAILED,
            EndpointRejectionReason.DNS_CONTAINS_PUBLIC_ADDRESS,
        )
    }

    private fun <T> runImmediateSuspend(block: suspend () -> T): T {
        var outcome: Result<T>? = null
        block.startCoroutine(
            object : Continuation<T> {
                override val context = EmptyCoroutineContext

                override fun resumeWith(result: Result<T>) {
                    outcome = result
                }
            },
        )
        return checkNotNull(outcome) {
            "The contract test policy must complete synchronously"
        }.getOrThrow()
    }
}
