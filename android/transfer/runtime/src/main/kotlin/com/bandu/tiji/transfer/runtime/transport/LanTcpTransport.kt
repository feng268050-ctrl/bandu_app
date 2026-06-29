package com.bandu.tiji.transfer.runtime.transport

import com.bandu.tiji.transfer.runtime.TransferRuntimeConfig
import java.io.Closeable
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket

class LanTcpTransport(
    private val addressProvider: LanAddressProvider = NetworkInterfaceLanAddressProvider(),
    private val serverSocketFactory: (InetAddress, Int) -> ServerSocket = { address, port ->
        ServerSocket().apply {
            reuseAddress = false
            bind(InetSocketAddress(address, port))
        }
    },
    private val socketFactory: () -> Socket = { Socket() },
    private val connectTimeoutMillis: Int = TransferRuntimeConfig.CONNECT_TIMEOUT_MILLIS.toInt(),
) {
    fun openServer(port: Int = 0): BoundTcpServer {
        require(port in 0..65_535) { "TCP port must be in range" }
        val address = addressProvider.bindableAddresses().firstOrNull()
            ?: throw NoLanAddressException()
        val socket = serverSocketFactory(address, port)
        return BoundTcpServer(socket, address, socket.localPort)
    }

    fun connect(
        address: InetAddress,
        port: Int,
    ): Socket {
        require(LanAddressPolicy.isAllowed(address)) { "Refusing non-LAN peer address: $address" }
        require(port in 1..65_535) { "TCP port must be in range" }
        return socketFactory().also { socket ->
            socket.connect(InetSocketAddress(address, port), connectTimeoutMillis)
        }
    }
}

data class BoundTcpServer(
    val socket: ServerSocket,
    val address: InetAddress,
    val port: Int,
) : Closeable {
    override fun close() {
        socket.close()
    }
}

class NoLanAddressException : IllegalStateException("No LAN address is available for transfer")

interface LanAddressProvider {
    fun bindableAddresses(): List<InetAddress>
}

class NetworkInterfaceLanAddressProvider : LanAddressProvider {
    override fun bindableAddresses(): List<InetAddress> =
        NetworkInterface.getNetworkInterfaces().asSequence()
            .filter { it.isUp && !it.isVirtual }
            .flatMap { networkInterface ->
                networkInterface.inetAddresses.asSequence()
            }
            .filter(LanAddressPolicy::isAllowed)
            .sortedWith(compareBy<InetAddress> { it.isLoopbackAddress }.thenBy { it.hostAddress })
            .toList()
}

object LanAddressPolicy {
    fun isAllowed(address: InetAddress): Boolean =
        !address.isAnyLocalAddress &&
            (address.isLoopbackAddress || address.isSiteLocalAddress || address.isLinkLocalAddress)
}
