package com.bandu.tiji.transfer.runtime.transport

import com.bandu.tiji.transfer.runtime.TransferRuntimeConfig
import com.google.common.truth.Truth.assertThat
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketAddress
import org.junit.Test

class LanTcpTransportTest {
    @Test
    fun `address policy rejects public and unspecified addresses`() {
        assertThat(LanAddressPolicy.isAllowed(InetAddress.getByName("0.0.0.0"))).isFalse()
        assertThat(LanAddressPolicy.isAllowed(InetAddress.getByName("8.8.8.8"))).isFalse()
        assertThat(LanAddressPolicy.isAllowed(InetAddress.getByName("127.0.0.1"))).isTrue()
        assertThat(LanAddressPolicy.isAllowed(InetAddress.getByName("192.168.1.10"))).isTrue()
        assertThat(LanAddressPolicy.isAllowed(InetAddress.getByName("10.0.0.5"))).isTrue()
        assertThat(LanAddressPolicy.isAllowed(InetAddress.getByName("169.254.10.2"))).isTrue()
    }

    @Test
    fun `server binds first allowed lan address instead of public address`() {
        val public = InetAddress.getByName("8.8.8.8")
        val lan = InetAddress.getByName("192.168.1.10")
        var requestedAddress: InetAddress? = null
        val transport = LanTcpTransport(
            addressProvider = object : LanAddressProvider {
                override fun bindableAddresses(): List<InetAddress> =
                    listOf(public, lan).filter(LanAddressPolicy::isAllowed)
            },
            serverSocketFactory = { address, _ ->
                requestedAddress = address
                ServerSocket()
            },
        )

        val server = transport.openServer()

        assertThat(server.address).isEqualTo(lan)
        assertThat(requestedAddress).isEqualTo(lan)
    }

    @Test(expected = NoLanAddressException::class)
    fun `server fails closed when no lan address is available`() {
        LanTcpTransport(
            addressProvider = object : LanAddressProvider {
                override fun bindableAddresses(): List<InetAddress> =
                    listOf(InetAddress.getByName("8.8.8.8")).filter(LanAddressPolicy::isAllowed)
            },
        ).openServer()
    }

    @Test
    fun `client refuses public peer address before opening socket`() {
        var createdSocket = false
        val transport = LanTcpTransport(
            socketFactory = {
                createdSocket = true
                CapturingSocket()
            },
        )

        val failure = runCatching {
            transport.connect(InetAddress.getByName("8.8.8.8"), 44321)
        }.exceptionOrNull()

        assertThat(failure).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(createdSocket).isFalse()
    }

    @Test
    fun `client uses configured connect timeout`() {
        val socket = CapturingSocket()
        val transport = LanTcpTransport(socketFactory = { socket })

        transport.connect(InetAddress.getByName("192.168.1.10"), 44321)

        assertThat(socket.endpoint).isEqualTo(InetSocketAddress(InetAddress.getByName("192.168.1.10"), 44321))
        assertThat(socket.timeoutMillis).isEqualTo(TransferRuntimeConfig.CONNECT_TIMEOUT_MILLIS.toInt())
    }
}

private class CapturingSocket : Socket() {
    var endpoint: SocketAddress? = null
    var timeoutMillis: Int? = null

    override fun connect(
        endpoint: SocketAddress?,
        timeout: Int,
    ) {
        this.endpoint = endpoint
        this.timeoutMillis = timeout
    }
}
