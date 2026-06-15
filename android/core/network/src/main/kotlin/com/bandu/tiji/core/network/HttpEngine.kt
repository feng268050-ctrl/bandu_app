package com.bandu.tiji.core.network

import okhttp3.Call
import okhttp3.Request

/**
 * Single entry point for creating network calls owned by this module.
 *
 * Callers inject the policy-protected client rather than constructing clients in feature code.
 */
class HttpEngine(
    private val callFactory: Call.Factory,
) {
    fun newCall(request: Request): Call = callFactory.newCall(request)
}
