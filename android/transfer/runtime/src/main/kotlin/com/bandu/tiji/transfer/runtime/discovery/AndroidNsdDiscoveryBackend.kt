package com.bandu.tiji.transfer.runtime.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import java.nio.charset.StandardCharsets

class AndroidNsdDiscoveryBackend(
    context: Context,
) : NsdDiscoveryBackend {
    private val nsdManager = context.getSystemService(NsdManager::class.java)
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private val resolving = mutableSetOf<String>()

    override var onPeerFound: ((NsdDiscoveredPeer) -> Unit)? = null

    override fun register(service: NsdPublishedService) {
        unregister()
        val info = NsdServiceInfo().apply {
            serviceName = service.serviceName
            serviceType = service.serviceType
            port = service.port
            service.txt.forEach { (key, value) -> setAttribute(key, value) }
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) = Unit

            override fun onRegistrationFailed(
                serviceInfo: NsdServiceInfo,
                errorCode: Int,
            ) = Unit

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) = Unit

            override fun onUnregistrationFailed(
                serviceInfo: NsdServiceInfo,
                errorCode: Int,
            ) = Unit
        }
        registrationListener = listener
        nsdManager.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    override fun unregister() {
        registrationListener?.let { listener ->
            runCatching { nsdManager.unregisterService(listener) }
        }
        registrationListener = null
    }

    override fun startDiscovery(serviceType: String) {
        stopDiscovery()
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) = Unit

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType != serviceType) return
                resolve(serviceInfo)
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit

            override fun onDiscoveryStopped(serviceType: String) = Unit

            override fun onStartDiscoveryFailed(
                serviceType: String,
                errorCode: Int,
            ) {
                runCatching { nsdManager.stopServiceDiscovery(this) }
            }

            override fun onStopDiscoveryFailed(
                serviceType: String,
                errorCode: Int,
            ) = Unit
        }
        discoveryListener = listener
        nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    override fun stopDiscovery() {
        discoveryListener?.let { listener ->
            runCatching { nsdManager.stopServiceDiscovery(listener) }
        }
        discoveryListener = null
        resolving.clear()
    }

    private fun resolve(serviceInfo: NsdServiceInfo) {
        val key = "${serviceInfo.serviceName}:${serviceInfo.serviceType}"
        if (!resolving.add(key)) return
        nsdManager.resolveService(
            serviceInfo,
            object : NsdManager.ResolveListener {
                override fun onResolveFailed(
                    serviceInfo: NsdServiceInfo,
                    errorCode: Int,
                ) {
                    resolving.remove(key)
                }

                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    resolving.remove(key)
                    parseNsdPeer(
                        serviceType = serviceInfo.serviceType,
                        txt = serviceInfo.attributes.mapValues { (_, value) ->
                            value.toString(StandardCharsets.UTF_8)
                        },
                        hostAddress = serviceInfo.host?.hostAddress.orEmpty(),
                        port = serviceInfo.port,
                    )?.let { onPeerFound?.invoke(it) }
                }
            },
        )
    }
}
