package com.bandu.tiji.core.network.endpoint

import okhttp3.HttpUrl

class DefaultEndpointPolicy(
    private val hostResolver: HostResolver = SystemHostResolver,
) : EndpointPolicy {
    override suspend fun validate(
        url: HttpUrl,
        allowPrivateCleartext: Boolean,
    ): EndpointDecision = when (url.scheme) {
        "https" -> EndpointDecision.Allowed
        "http" -> validateCleartext(url, allowPrivateCleartext)
        else -> EndpointDecision.Rejected(EndpointRejectionReason.UNSUPPORTED_SCHEME)
    }

    private suspend fun validateCleartext(
        url: HttpUrl,
        allowPrivateCleartext: Boolean,
    ): EndpointDecision {
        if (!allowPrivateCleartext) {
            return EndpointDecision.Rejected(
                EndpointRejectionReason.CLEARTEXT_NOT_CONFIRMED,
            )
        }
        val address = PrivateAddressClassifier.parseIpLiteral(url.host)
        if (PrivateAddressClassifier.isLocalhost(url.host)) {
            return EndpointDecision.Allowed
        }
        if (address != null) {
            return if (PrivateAddressClassifier.isAllowed(address)) {
                EndpointDecision.Allowed
            } else {
                EndpointDecision.Rejected(
                    EndpointRejectionReason.CLEARTEXT_HOST_NOT_PRIVATE,
                )
            }
        }

        val resolved = try {
            hostResolver.resolve(url.host)
        } catch (_: Exception) {
            return EndpointDecision.Rejected(
                EndpointRejectionReason.DNS_RESOLUTION_FAILED,
            )
        }
        if (resolved.isEmpty()) {
            return EndpointDecision.Rejected(
                EndpointRejectionReason.DNS_RESOLUTION_FAILED,
            )
        }
        return if (resolved.all(PrivateAddressClassifier::isAllowed)) {
            EndpointDecision.Allowed
        } else {
            EndpointDecision.Rejected(
                EndpointRejectionReason.DNS_CONTAINS_PUBLIC_ADDRESS,
            )
        }
    }
}
