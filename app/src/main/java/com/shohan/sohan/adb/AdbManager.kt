package com.shohan.sohan.adb

import dadb.AdbKeyPair
import dadb.Dadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.security.KeyPairGenerator

object AdbManager {

    private var dadb: Dadb? = null

    val isConnected: Boolean get() = dadb != null

    suspend fun connect(host: String, port: Int, keyDir: File): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                dadb?.close()
                dadb = null

                val connection = try {
                    val keyPair = loadOrCreate(keyDir)
                    Dadb.create(host, port, keyPair)
                } catch (_: Exception) {
                    Dadb.create(host, port)
                }

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

    suspend fun shell(command: String): Result<String> =
        withContext(Dispatchers.IO) {
            val conn = dadb
                ?: return@withContext Result.failure(Exception("Not connected to ADB."))
            try {
                val response = conn.shell(command)
                Result.success(response.output)
            } catch (e: Exception) {
                dadb = null
                Result.failure(Exception("Shell failed: \${e.message}"))
            }
        }

    private fun loadOrCreate(keyDir: File): AdbKeyPair {
        keyDir.mkdirs()
        val priv = File(keyDir, "sohan_adb_key")
        val pub = File(keyDir, "sohan_adb_key.pub")
        if (!priv.exists() || !pub.exists()) {
            val kg = KeyPairGenerator.getInstance("RSA")
            kg.initialize(2048)
            val kp = kg.generateKeyPair()
            priv.writeBytes(kp.private.encoded)
            pub.writeBytes(kp.public.encoded)
        }
        return AdbKeyPair.read(priv, pub)
    }
}
