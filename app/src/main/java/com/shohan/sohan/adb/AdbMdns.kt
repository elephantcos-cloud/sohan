package com.shohan.sohan.adb
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.Observer
import java.io.IOException
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
@RequiresApi(Build.VERSION_CODES.R)
class AdbMdns(context: Context, private val serviceType: String, private val observer: Observer<Int>) {
    private var registered = false; private var running = false; private var serviceName: String? = null
    private val nsd = context.getSystemService(NsdManager::class.java)
    private val listener = Disc(this)
    fun start() { if (running) return; running = true; nsd.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener) }
    fun stop()  { if (!running) return; running = false; if (registered) nsd.stopServiceDiscovery(listener) }
    private fun onStart() { registered = true }
    private fun onStop()  { registered = false }
    private fun onFound(info: NsdServiceInfo) { nsd.resolveService(info, Res(this)) }
    private fun onLost(info: NsdServiceInfo)  { if (info.serviceName == serviceName) observer.onChanged(-1) }
    private fun onResolved(info: NsdServiceInfo) {
        if (!running) return
        val onThis = NetworkInterface.getNetworkInterfaces()?.asSequence()
            ?.flatMap { it.inetAddresses.asSequence() }
            ?.any { it.hostAddress == info.host?.hostAddress } == true
        if (onThis && isBound(info.port)) { serviceName = info.serviceName; observer.onChanged(info.port) }
    }
    private fun isBound(port: Int) = try { ServerSocket().use { it.bind(InetSocketAddress("127.0.0.1", port), 1) }; false } catch (_: IOException) { true }
    private class Disc(private val m: AdbMdns) : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(t: String) { m.onStart() }
        override fun onDiscoveryStopped(t: String) { m.onStop()  }
        override fun onStartDiscoveryFailed(t: String, e: Int) = Unit
        override fun onStopDiscoveryFailed(t: String, e: Int)  = Unit
        override fun onServiceFound(i: NsdServiceInfo) { m.onFound(i) }
        override fun onServiceLost(i: NsdServiceInfo)  { m.onLost(i)  }
    }
    private class Res(private val m: AdbMdns) : NsdManager.ResolveListener {
        override fun onResolveFailed(i: NsdServiceInfo, e: Int) = Unit
        override fun onServiceResolved(i: NsdServiceInfo) { m.onResolved(i) }
    }
    companion object { const val TLS_CONNECT = "_adb-tls-connect._tcp"; const val TLS_PAIRING = "_adb-tls-pairing._tcp" }
}
