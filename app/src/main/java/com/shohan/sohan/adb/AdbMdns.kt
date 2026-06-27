package com.shohan.sohan.adb

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.Observer
import java.io.IOException
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket

@RequiresApi(Build.VERSION_CODES.R)
class AdbMdns(
    context: Context,
    private val serviceType: String,
    private val observer: Observer<Int>
) {
    private var registered = false
    private var running    = false
    private var serviceName: String? = null
    private val nsd = context.getSystemService(NsdManager::class.java)
    private val listener = DiscoveryListener(this)

    fun start() {
        if (running) return
        running = true
        nsd.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    fun stop() {
        if (!running) return
        running = false
        if (registered) nsd.stopServiceDiscovery(listener)
    }

    private fun onDiscoveryStart()  { registered = true  }
    private fun onDiscoveryStop()   { registered = false }

    private fun onServiceFound(info: NsdServiceInfo) {
        nsd.resolveService(info, ResolveListener(this))
    }

    private fun onServiceLost(info: NsdServiceInfo) {
        if (info.serviceName == serviceName) observer.onChanged(-1)
    }

    private fun onServiceResolved(info: NsdServiceInfo) {
        if (!running) return
        // Verify the service is on THIS device
        val onThisDevice = NetworkInterface.getNetworkInterfaces()
            ?.asSequence()
            ?.flatMap { it.inetAddresses.asSequence() }
            ?.any { it.hostAddress == info.host?.hostAddress } == true
        if (onThisDevice && isPortBound(info.port)) {
            serviceName = info.serviceName
            observer.onChanged(info.port)
        }
    }

    // True if something is already listening on that port (i.e., adbd is active)
    private fun isPortBound(port: Int) = try {
        ServerSocket().use { it.bind(InetSocketAddress("127.0.0.1", port), 1); false }
    } catch (_: IOException) { true }

    private class DiscoveryListener(private val mdns: AdbMdns) : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(t: String)          { mdns.onDiscoveryStart() }
        override fun onDiscoveryStopped(t: String)          { mdns.onDiscoveryStop()  }
        override fun onStartDiscoveryFailed(t: String, e: Int) = Unit
        override fun onStopDiscoveryFailed(t: String, e: Int)  = Unit
        override fun onServiceFound(info: NsdServiceInfo)   { mdns.onServiceFound(info) }
        override fun onServiceLost(info: NsdServiceInfo)    { mdns.onServiceLost(info)  }
    }

    private class ResolveListener(private val mdns: AdbMdns) : NsdManager.ResolveListener {
        override fun onResolveFailed(i: NsdServiceInfo, e: Int) = Unit
        override fun onServiceResolved(i: NsdServiceInfo) { mdns.onServiceResolved(i) }
    }

    companion object {
        const val TLS_CONNECT = "_adb-tls-connect._tcp"
        const val TLS_PAIRING = "_adb-tls-pairing._tcp"
    }
}
