package com.bandu.tiji.core.network.endpoint

import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response

class EndpointRejectedException(
    val rejectionReason: EndpointRejectionReason,
) : IOException("Endpoint rejected: $rejectionReason")

class EndpointPolicyInterceptor(
    private val endpointPolicy: EndpointPolicy,
    private val allowPrivateCleartext: () -> Boolean,
    private val validateRedirectLocation: Boolean,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        validate(chain.request().url)
        val response = chain.proceed(chain.request())
        if (validateRedirectLocation && response.isRedirect) {
            response.header("Location")
                ?.let(response.request.url::resolve)
                ?.let(::validate)
        }
        return response
    }

    private fun validate(url: okhttp3.HttpUrl) {
        val decision = runBlocking {
            endpointPolicy.validate(url, allowPrivateCleartext())
        }
        if (decision is EndpointDecision.Rejected) {
            throw EndpointRejectedException(decision.reason)
        }
    }
}

fun OkHttpClient.Builder.enforceEndpointPolicy(
    endpointPolicy: EndpointPolicy,
    allowPrivateCleartext: () -> Boolean,
): OkHttpClient.Builder = apply {
    addInterceptor(
        EndpointPolicyInterceptor(
            endpointPolicy = endpointPolicy,
            allowPrivateCleartext = allowPrivateCleartext,
            validateRedirectLocation = false,
        ),
    )
    addNetworkInterceptor(
        EndpointPolicyInterceptor(
            endpointPolicy = endpointPolicy,
            allowPrivateCleartext = allowPrivateCleartext,
            validateRedirectLocation = true,
        ),
    )
}
