package com.bandu.tiji.transfer.runtime.discovery

import com.bandu.tiji.domain.transfer.DiscoveryMode
import com.bandu.tiji.transfer.protocol.identity.IdentityProof
import com.bandu.tiji.transfer.runtime.TransferRuntimeConfig
import java.net.Inet6Address
import java.net.InetAddress

data class ManualDiscoveryEndpoint(
    val hostAddress: String,
    val port: Int,
) {
    init {
        require(hostAddress.isNotBlank())
        require(port in 1..65_535)
    }

    fun toPeer(
        displayName: String,
        mode: DiscoveryMode = DiscoveryMode.PAIR,
    ): NsdDiscoveredPeer {
        val peerKey = "$hostAddress:$port"
        return NsdDiscoveredPeer(
            discoveryId = IdentityProof.fingerprint(peerKey.encodeToByteArray()).take(12),
            displayName = displayName.limitUtf8Bytes(TransferRuntimeConfig.MAX_DEVICE_NAME_BYTES),
            mode = mode,
            fingerprintPrefix = IdentityProof.fingerprint("manual:$peerKey".encodeToByteArray()).take(12),
            hostAddress = hostAddress,
            port = port,
        )
    }
}

fun parseManualDiscoveryEndpoint(
    input: String,
    defaultPort: Int,
): ManualDiscoveryEndpoint {
    require(defaultPort in 1..65_535)
    val trimmed = input.trim()
    require(trimmed.isNotBlank()) { "Manual discovery address is required" }
    val (host, port) = parseHostAndPort(trimmed, defaultPort)
    require(isAllowedManualHost(host)) { "Manual discovery only accepts private or local IP addresses" }
    return ManualDiscoveryEndpoint(hostAddress = host, port = port)
}

private fun parseHostAndPort(
    input: String,
    defaultPort: Int,
): Pair<String, Int> {
    if (input.startsWith("[")) {
        val hostEnd = input.indexOf(']')
        require(hostEnd > 1) { "Invalid bracketed IPv6 address" }
        val host = input.substring(1, hostEnd)
        val port = if (hostEnd == input.lastIndex) {
            defaultPort
        } else {
            require(input.getOrNull(hostEnd + 1) == ':') { "Invalid bracketed IPv6 port" }
            parsePort(input.substring(hostEnd + 2))
        }
        return host to port
    }

    val colonCount = input.count { it == ':' }
    return when {
        colonCount == 0 -> input to defaultPort
        colonCount == 1 -> {
            val host = input.substringBefore(':')
            val port = parsePort(input.substringAfter(':'))
            host to port
        }
        else -> input to defaultPort
    }
}

private fun parsePort(value: String): Int {
    require(value.isNotBlank()) { "Port is required" }
    require(value.all(Char::isDigit)) { "Port must be numeric" }
    val port = value.toInt()
    require(port in 1..65_535) { "Port must be within TCP port range" }
    return port
}

private fun isAllowedManualHost(host: String): Boolean =
    parseIpv4(host)?.let(::isAllowedIpv4) ?: isAllowedIpv6(host)

private fun parseIpv4(host: String): IntArray? {
    val parts = host.split('.')
    if (parts.size != 4) return null
    return parts.map { part ->
        if (part.isEmpty() || part.length > 3 || !part.all(Char::isDigit)) return null
        part.toIntOrNull()?.takeIf { it in 0..255 } ?: return null
    }.toIntArray()
}

private fun isAllowedIpv4(parts: IntArray): Boolean {
    val first = parts[0]
    val second = parts[1]
    return first == 10 ||
        first == 127 ||
        first == 192 && second == 168 ||
        first == 172 && second in 16..31 ||
        first == 169 && second == 254
}

private fun isAllowedIpv6(host: String): Boolean {
    if (!host.any { it == ':' }) return false
    val normalized = host.substringBefore('%')
    if (!normalized.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' || it == ':' || it == '.' }) {
        return false
    }
    val address = runCatching { InetAddress.getByName(host) }.getOrNull() as? Inet6Address ?: return false
    val firstByte = address.address.first().toInt() and 0xff
    return address.isLoopbackAddress ||
        address.isLinkLocalAddress ||
        firstByte == 0xfc ||
        firstByte == 0xfd
}
