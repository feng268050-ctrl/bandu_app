package com.bandu.tiji.core.network.endpoint

import java.net.InetAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun interface HostResolver {
    suspend fun resolve(host: String): List<InetAddress>
}

object SystemHostResolver : HostResolver {
    override suspend fun resolve(host: String): List<InetAddress> =
        withContext(Dispatchers.IO) {
            InetAddress.getAllByName(host).toList()
        }
}
