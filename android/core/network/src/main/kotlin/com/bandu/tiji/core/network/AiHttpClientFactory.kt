package com.bandu.tiji.core.network

import com.bandu.tiji.core.common.logging.AppLogger
import com.bandu.tiji.core.network.endpoint.EndpointPolicy
import com.bandu.tiji.core.network.endpoint.enforceEndpointPolicy
import com.bandu.tiji.core.network.logging.RedactingNetworkLoggingInterceptor
import java.time.Duration
import okhttp3.OkHttpClient

enum class AiHttpOperation(
    val totalTimeout: Duration,
) {
    CONFIGURATION_VALIDATION(Duration.ofSeconds(20)),
    IMAGE_ANALYSIS(Duration.ofSeconds(180)),
    TUTOR_STREAM(Duration.ofMinutes(10)),
    EXERCISE(Duration.ofSeconds(120)),
}

data class AiHttpTimeouts(
    val connect: Duration = Duration.ofSeconds(10),
    val read: Duration = Duration.ofSeconds(60),
    val write: Duration = Duration.ofSeconds(60),
    val call: Duration,
) {
    init {
        require(!connect.isZero && !connect.isNegative)
        require(!read.isZero && !read.isNegative)
        require(!write.isZero && !write.isNegative)
        require(!call.isZero && !call.isNegative)
    }
}

object AiHttpClientFactory {
    fun create(
        operation: AiHttpOperation,
        endpointPolicy: EndpointPolicy,
        allowPrivateCleartext: () -> Boolean,
        logger: AppLogger? = null,
    ): OkHttpClient = create(
        timeouts = AiHttpTimeouts(call = operation.totalTimeout),
        endpointPolicy = endpointPolicy,
        allowPrivateCleartext = allowPrivateCleartext,
        logger = logger,
    )

    fun create(
        timeouts: AiHttpTimeouts,
        endpointPolicy: EndpointPolicy,
        allowPrivateCleartext: () -> Boolean,
        logger: AppLogger? = null,
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(timeouts.connect)
            .readTimeout(timeouts.read)
            .writeTimeout(timeouts.write)
            .callTimeout(timeouts.call)
            .retryOnConnectionFailure(false)
            .enforceEndpointPolicy(endpointPolicy, allowPrivateCleartext)
        if (logger != null) {
            builder.addInterceptor(RedactingNetworkLoggingInterceptor(logger))
        }
        return builder.build()
    }
}
