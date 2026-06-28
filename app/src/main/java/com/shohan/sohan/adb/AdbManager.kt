package com.shohan.sohan.adb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.ConnectException
import java.net.SocketTimeoutException
object AdbManager {
    private var client: AdbClient? = null
    val isConnected: Boolean get() = client != null
    suspend fun connect(host: String, port: Int, @Suppress("UNUSED_PARAMETER") keyDir: File): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                client?.close(); client = null
                AdbKey.ensureKey()
                val c = AdbClient(host, port)
                c.connect()
                val out = c.shell("echo sohan_ok")
                if (!out.contains("sohan_ok")) { c.close(); return@withContext Result.failure(Exception("Shell test failed")) }
                client = c; Result.success(Unit)
            } catch (e: ConnectException) { Result.failure(Exception("Connection refused — Wireless Debugging চালু আছে? Port: \$port")) }
            catch (e: SocketTimeoutException) { Result.failure(Exception("Timeout. Port ঠিক আছে?")) }
            catch (e: AdbException) { Result.failure(Exception(e.message ?: "ADB error")) }
            catch (e: Exception) { Result.failure(Exception(e.message ?: "Unknown error")) }
        }
    fun disconnect() { client?.close(); client = null }
    suspend fun shell(command: String): Result<String> =
        withContext(Dispatchers.IO) {
            val c = client ?: return@withContext Result.failure(Exception("Not connected."))
            try { Result.success(c.shell(command)) }
            catch (e: Exception) { client = null; Result.failure(Exception("Shell failed: \${e.message}")) }
        }
}
