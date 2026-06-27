package com.shohan.sohan.adb

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal object AdbMdns {
    data class ServiceInfo(val host: String, val port: Int)
    private const val PAIRING_SERVICE = "_adb-tls-pairing._tcp"

    fun pairingServiceFlow(context: Context): Flow<ServiceInfo?> = callbackFlow {
        val nsd = context.getSystemService(NsdManager::class.java)
        val listener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(s: String, e: Int) {}
            override fun onStopDiscoveryFailed(s: String, e: Int)  {}
            override fun onDiscoveryStarted(s: String)             {}
            override fun onDiscoveryStopped(s: String)             {}
            override fun onServiceLost(info: NsdServiceInfo) { trySend(null) }
            override fun onServiceFound(info: NsdServiceInfo) {
                nsd.resolveService(info, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(i: NsdServiceInfo, e: Int) {}
                    override fun onServiceResolved(i: NsdServiceInfo) {
                        val host = i.host?.hostAddress ?: return
                        trySend(ServiceInfo(host, i.port))
                    }
                })
            }
        }
        nsd.discoverServices(PAIRING_SERVICE, NsdManager.PROTOCOL_DNS_SD, listener)
        awaitClose { nsd.stopServiceDiscovery(listener) }
    }
}
