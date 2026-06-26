package com.shohan.sohan.adb

import dadb.AdbKeyPair
import dadb.Dadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.ConnectException
import java.net.SocketTimeoutException

/**
 * Singleton owning the single ADB TCP connection for the whole app.
 *
 * Connection flow:
 *  1. App generates an RSA key pair on first launch (stored in filesDir).
 *  2. [connect] opens a TCP connection to the device's Wireless ADB daemon.
 *  3. Android shows "Allow ADB debugging?" — user taps Allow.
 *  4. All [shell] calls run with shell-user permissions.
 */
object AdbManager {

    private var dadb: Dadb? = null

    val isConnected: Boolean get() = dadb != null

    // ── Connect / disconnect ──────────────────────────────────────────────────

    suspend fun connect(host: String, port: Int, keyDir: File): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                dadb?.close()
                dadb = null

                // Try with key pair first, fall back to no-key for older ADB
                val connection = try {
                    val keyPair = loadOrCreate(keyDir)
                    Dadb.create(host, port, keyPair)
                } catch (_: Exception) {
                    Dadb.create(host, port)
                }

                // Smoke test
                val probe = connection.shell("echo sohan_ok")
                if (!probe.output.contains("sohan_ok")) {
                    connection.close()
                    return@withContext Result.failure(
                        Exception("Connected but shell test failed. Try restarting Wireless Debugging.")
                    )
                }

                dadb = connection
                Result.success(Unit)
            } catch (e: ConnectException) {
                Result.failure(Exception("Connection refused — is Wireless Debugging enabled? Port: $port"))
            } catch (e: SocketTimeoutException) {
                Result.failure(Exception("Connection timed out. Check the port number."))
            } catch (e: Exception) {
                Result.failure(Exception(e.message ?: "Unknown ADB error"))
            }
        }

    fun disconnect() {
        dadb?.close()
        dadb = null
    }

    // ── Shell execution ───────────────────────────────────────────────────────

    suspend fun shell(command: String): Result<String> =
        withContext(Dispatchers.IO) {
            val conn = dadb
                ?: return@withContext Result.failure(Exception("Not connected to ADB."))
            try {
                val response = conn.shell(command)
                Result.success(response.output)
            } catch (e: Exception) {
                // Session died
                dadb = null
                Result.failure(Exception("Shell failed: ${e.message}"))
            }
        }

    // ── Key pair ──────────────────────────────────────────────────────────────

    private fun loadOrCreate(keyDir: File): AdbKeyPair {
        keyDir.mkdirs()
        val priv = File(keyDir, "sohan_adb_key")
        val pub  = File(keyDir, "sohan_adb_key.pub")
            val kg = java.security.KeyPairGenerator.getInstance("RSA")
            kg.initialize(2048)
            val kp = kg.generateKeyPair()
            priv.writeBytes(kp.private.encoded)
            pub.writeBytes(kp.public.encoded)
        }
        return AdbKeyPair.read(priv, pub)
    }
}
