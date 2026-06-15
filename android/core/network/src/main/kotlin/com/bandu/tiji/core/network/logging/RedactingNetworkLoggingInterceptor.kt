package com.bandu.tiji.core.network.logging

import com.bandu.tiji.core.common.logging.AppLogger
import java.io.IOException
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response

class RedactingNetworkLoggingInterceptor(
    private val logger: AppLogger,
    private val nanoTime: () -> Long = System::nanoTime,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startedAt = nanoTime()
        val requestFields = mapOf(
            "method" to request.method,
            "endpoint" to request.url.redactedOrigin(),
        )
        logger.debug("network_request", requestFields)

        return try {
            val response = chain.proceed(request)
            logger.info(
                "network_response",
                requestFields + mapOf(
                    "durationMs" to elapsedMillis(startedAt),
                    "statusCategory" to response.code.statusCategory(),
                ),
            )
            response
        } catch (error: IOException) {
            logger.warn(
                "network_failure",
                requestFields + mapOf(
                    "durationMs" to elapsedMillis(startedAt),
                    "errorType" to error.javaClass.simpleName,
                ),
            )
            throw error
        }
    }

    private fun elapsedMillis(startedAt: Long): Long =
        ((nanoTime() - startedAt).coerceAtLeast(0L)) / NANOS_PER_MILLISECOND
}

private fun HttpUrl.redactedOrigin(): String = buildString {
    append(scheme)
    append("://")
    append(host)
    if (port != defaultPort(scheme)) {
        append(':')
        append(port)
    }
}

private fun Int.statusCategory(): String = "${this / 100}xx"

private fun defaultPort(scheme: String): Int = when (scheme) {
    "http" -> 80
    "https" -> 443
    else -> -1
}

private const val NANOS_PER_MILLISECOND = 1_000_000L
