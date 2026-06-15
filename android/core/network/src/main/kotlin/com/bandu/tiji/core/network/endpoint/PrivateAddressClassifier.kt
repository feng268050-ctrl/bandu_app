package com.bandu.tiji.core.network.endpoint

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

object PrivateAddressClassifier {
    fun isLocalhost(host: String): Boolean =
        host.equals("localhost", ignoreCase = true)

    fun isAllowed(address: InetAddress): Boolean = when (address) {
        is Inet4Address -> isAllowedIpv4(address.address)
        is Inet6Address -> isAllowedIpv6(address.address)
        else -> false
    }

    fun parseIpLiteral(host: String): InetAddress? = when {
        host.contains(':') -> runCatching { InetAddress.getByName(host) }.getOrNull()
        IPV4_LITERAL.matches(host) -> {
            val octets = host.split('.').map(String::toInt)
            if (octets.all { it in 0..255 }) {
                InetAddress.getByAddress(octets.map(Int::toByte).toByteArray())
            } else {
                null
            }
        }
        else -> null
    }

    private fun isAllowedIpv4(bytes: ByteArray): Boolean {
        val first = bytes[0].toInt() and 0xff
        val second = bytes[1].toInt() and 0xff
        return first == 127 ||
            first == 10 ||
            (first == 172 && second in 16..31) ||
            (first == 192 && second == 168)
    }

    private fun isAllowedIpv6(bytes: ByteArray): Boolean {
        if (bytes.contentEquals(IPV6_LOOPBACK)) return true
        val first = bytes[0].toInt() and 0xff
        val second = bytes[1].toInt() and 0xff
        val isUniqueLocal = first and 0xfe == 0xfc
        val isLinkLocal = first == 0xfe && second and 0xc0 == 0x80
        return isUniqueLocal || isLinkLocal
    }

    private val IPV4_LITERAL = Regex("""\d{1,3}(?:\.\d{1,3}){3}""")
    private val IPV6_LOOPBACK = ByteArray(16).apply { this[15] = 1 }
}
