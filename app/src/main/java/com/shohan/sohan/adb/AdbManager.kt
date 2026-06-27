package com.shohan.sohan.adb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.ConnectException
import java.net.SocketTimeoutException

/**
 * Singleton managing the ADB connection.
 * Uses Shizuku-style AdbClient (raw ADB protocol) instead of dadb library.
 * Android Keystore handles RSA key generation — no external crypto library needed.
 */
object AdbManager {

    private var client: AdbClient? = null

    val isConnected: Boolean get() = client != null

    // keyDir kept for API compatibility with MainViewModel — not used
    suspend fun connect(host: String, port: Int, @Suppress("UNUSED_PARAMETER") keyDir: File): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                client?.close()
                client = null

                AdbKey.ensureKey()          // generate RSA key if first run

                val c = AdbClient(host, port)
                c.connect()

                // Smoke-test
                val out = c.shell("echo sohan_ok")
                if (!out.contains("sohan_ok")) {
                    c.close()
                    return@withContext Result.failure(
                        Exception("Connected but shell test failed. Restart Wireless Debugging.")
                    )
                }

                client = c
                Result.success(Unit)
            } catch (e: ConnectException) {
                Result.failure(Exception("Connection refused — is Wireless Debugging on? Port: $port"))
            } catch (e: SocketTimeoutException) {
                Result.failure(Exception("Connection timed out. Check port number."))
            } catch (e: AdbException) {
                Result.failure(Exception(e.message ?: "ADB error"))
            } catch (e: Exception) {
                Result.failure(Exception(e.message ?: "Unknown error"))
            }
        }

    fun disconnect() {
        client?.close()
        client = null
    }

    suspend fun shell(command: String): Result<String> =
        withContext(Dispatchers.IO) {
            val c = client ?: return@withContext Result.failure(Exception("Not connected to ADB."))
            try {
                Result.success(c.shell(command))
            } catch (e: Exception) {
                client = null
                Result.failure(Exception("Shell failed: ${e.message}"))
            }
        }
}
