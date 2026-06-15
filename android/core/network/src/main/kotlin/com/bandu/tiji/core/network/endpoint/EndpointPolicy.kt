package com.bandu.tiji.core.network.endpoint

import okhttp3.HttpUrl

/**
 * Validates every endpoint before a request or redirect is allowed to connect.
 */
fun interface EndpointPolicy {
    suspend fun validate(
        url: HttpUrl,
        allowPrivateCleartext: Boolean,
    ): EndpointDecision
}

sealed interface EndpointDecision {
    data object Allowed : EndpointDecision

    data class Rejected(
        val reason: EndpointRejectionReason,
    ) : EndpointDecision
}

enum class EndpointRejectionReason {
    UNSUPPORTED_SCHEME,
    CLEARTEXT_NOT_CONFIRMED,
    CLEARTEXT_HOST_NOT_PRIVATE,
    DNS_RESOLUTION_FAILED,
    DNS_CONTAINS_PUBLIC_ADDRESS,
}
