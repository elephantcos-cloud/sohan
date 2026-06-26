package com.shohan.sohan.client

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.shohan.sohan.ISohanService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Easy-to-use Kotlin client for the Sohan ADB bridge.
 *
 * ## Usage
 *
 * ```kotlin
 * val client = SohanClient(context)
 * client.connect()           // binds to SohanService
 *
 * val output = client.shell("pm list packages -3")
 * println(output)
 *
 * client.clearCache("com.example.app")
 * client.forceStop("com.example.app")
 *
 * client.disconnect()        // call in onDestroy / onStop
 * ```
 *
 * If Sohan is not installed or ADB is not connected, every method returns
 * a [SohanResult.Error] — no exceptions thrown.
 */
class SohanClient(private val context: Context) {

    companion object {
        private const val SOHAN_PACKAGE = "com.shohan.sohan"
        private const val SOHAN_ACTION  = "com.shohan.sohan.SERVICE"
        private const val CONNECT_TIMEOUT_MS = 5_000L
    }

    // ── Internal state ────────────────────────────────────────────────────────

    private var service: ISohanService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = ISohanService.Stub.asInterface(binder)
            isBound = true
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            isBound = false
        }
    }

    // ── Connection ────────────────────────────────────────────────────────────

    /**
     * Binds to SohanService. Suspends until the connection is ready or times out.
     *
     * @return [SohanResult.Success] with the Sohan service version,
     *         or [SohanResult.Error] if Sohan is not installed / not responding.
     */
    suspend fun connect(): SohanResult<Int> = withContext(Dispatchers.Main) {
        if (isBound && service != null) {
            return@withContext SohanResult.Success(service!!.version)
        }

        val intent = Intent(SOHAN_ACTION).apply { setPackage(SOHAN_PACKAGE) }
        val bound = try {
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            return@withContext SohanResult.Error(
                "Could not bind to Sohan: ${e.message}",
                SohanError.NOT_INSTALLED
            )
        }

        if (!bound) {
            return@withContext SohanResult.Error(
                "Sohan is not installed. Install it from https://github.com/elephantcos-cloud/sohan",
                SohanError.NOT_INSTALLED
            )
        }

        // Wait until onServiceConnected fires or timeout
        val connected = withTimeoutOrNull(CONNECT_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                // Poll since ServiceConnection has no callback we can hook
                val thread = Thread {
                    repeat(50) {
                        if (isBound && service != null) {
                            cont.resume(true)
                            return@Thread
                        }
                        Thread.sleep(100)
                    }
                    cont.resume(false)
                }
                thread.start()
                cont.invokeOnCancellation { thread.interrupt() }
            }
        }

        if (connected == true && service != null) {
            SohanResult.Success(service!!.getVersion())
        } else {
            SohanResult.Error("Connection timed out.", SohanError.TIMEOUT)
        }
    }

    /** Unbinds from SohanService. Call this in onDestroy / onStop. */
    fun disconnect() {
        if (isBound) {
            try { context.unbindService(connection) } catch (_: Exception) {}
            isBound = false
            service = null
        }
    }

    // ── Status ────────────────────────────────────────────────────────────────

    /** True if this client is currently bound to SohanService. */
    val isConnected: Boolean get() = isBound && service != null

    /**
     * True if Sohan's internal ADB TCP session is alive.
     * Even if the client is bound, ADB might be disconnected.
     */
    fun isAdbConnected(): Boolean = runCatching { service?.isAdbConnected() }.getOrNull() ?: false

    // ── Permission ────────────────────────────────────────────────────────────

    /**
     * Returns true if this app is authorized to use Sohan.
     * If not authorized, call [requestPermission] first.
     */
    fun hasPermission(): Boolean = runCatching { service?.hasPermission() }.getOrNull() ?: false

    /**
     * Asks Sohan to show a permission dialog to the user.
     * After the user taps Allow, retry your shell/action call.
     */
    fun requestPermission() = runCatching { service?.requestPermission() }

    // ── Shell ─────────────────────────────────────────────────────────────────

    /**
     * Runs [command] in an ADB shell (shell-user permissions).
     *
     * @return [SohanResult.Success] with stdout,
     *         or [SohanResult.Error] with the reason.
     */
    suspend fun shell(command: String): SohanResult<String> = withContext(Dispatchers.IO) {
        val svc = service ?: return@withContext notBound()
        runCatching {
            SohanResult.Success(svc.shell(command))
        }.getOrElse { e -> mapException(e) }
    }

    // ── Privileged actions ────────────────────────────────────────────────────

    /**
     * Force-stops [packageName].
     */
    suspend fun forceStop(packageName: String): SohanResult<Unit> =
        withContext(Dispatchers.IO) {
            val svc = service ?: return@withContext notBound()
            runCatching {
                svc.forceStop(packageName)
                SohanResult.Success(Unit)
            }.getOrElse { e -> mapException(e) }
        }

    /**
     * Clears the cache of [packageName] only — data is NOT touched.
     *
     * @return [SohanResult.Success] with "Success",
     *         or [SohanResult.Error] on failure.
     */
    suspend fun clearCache(packageName: String): SohanResult<String> =
        withContext(Dispatchers.IO) {
            val svc = service ?: return@withContext notBound()
            runCatching {
                val result = svc.clearCache(packageName)
                if (result.startsWith("Success", ignoreCase = true))
                    SohanResult.Success(result)
                else
                    SohanResult.Error(result, SohanError.ACTION_FAILED)
            }.getOrElse { e -> mapException(e) }
        }

    /**
     * Uninstalls [packageName] for the current user.
     */
    suspend fun uninstall(packageName: String): SohanResult<String> =
        withContext(Dispatchers.IO) {
            val svc = service ?: return@withContext notBound()
            runCatching {
                val result = svc.uninstall(packageName)
                if (result.startsWith("Success", ignoreCase = true))
                    SohanResult.Success(result)
                else
                    SohanResult.Error(result, SohanError.ACTION_FAILED)
            }.getOrElse { e -> mapException(e) }
        }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun <T> notBound(): SohanResult<T> =
        SohanResult.Error("Not connected to Sohan. Call connect() first.", SohanError.NOT_BOUND)

    private fun <T> mapException(e: Throwable): SohanResult<T> = when {
        e is SecurityException ->
            SohanResult.Error(
                "Not authorized. Call requestPermission() and wait for the user to Allow.",
                SohanError.NOT_AUTHORIZED
            )
        e.message?.contains("ADB is not connected") == true ->
            SohanResult.Error(
                "Sohan's ADB session is down. Open Sohan to reconnect.",
                SohanError.ADB_DISCONNECTED
            )
        else ->
            SohanResult.Error(e.message ?: "Unknown error", SohanError.UNKNOWN)
    }
}
