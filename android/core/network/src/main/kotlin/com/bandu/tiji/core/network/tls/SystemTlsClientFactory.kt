package com.bandu.tiji.core.network.tls

import okhttp3.OkHttpClient

/**
 * Creates clients backed exclusively by the platform trust store and hostname verifier.
 */
object SystemTlsClientFactory {
    fun create(): OkHttpClient = OkHttpClient.Builder().build()
}
