package com.shohan.sohan.adb

import android.util.Log
import com.shohan.sohan.adb.AdbProtocol.ADB_AUTH_RSAPUBLICKEY
import com.shohan.sohan.adb.AdbProtocol.ADB_AUTH_SIGNATURE
import com.shohan.sohan.adb.AdbProtocol.ADB_AUTH_TOKEN
import com.shohan.sohan.adb.AdbProtocol.A_AUTH
import com.shohan.sohan.adb.AdbProtocol.A_CLSE
import com.shohan.sohan.adb.AdbProtocol.A_CNXN
import com.shohan.sohan.adb.AdbProtocol.A_MAXDATA
import com.shohan.sohan.adb.AdbProtocol.A_OKAY
import com.shohan.sohan.adb.AdbProtocol.A_OPEN
import com.shohan.sohan.adb.AdbProtocol.A_STLS
import com.shohan.sohan.adb.AdbProtocol.A_STLS_VERSION
import com.shohan.sohan.adb.AdbProtocol.A_VERSION
import com.shohan.sohan.adb.AdbProtocol.A_WRTE
import java.io.Closeable
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.net.ssl.SSLSocket

private const val TAG = "AdbClient"

/**
 * Raw ADB protocol client — adapted from Shizuku.
 * Supports both TLS (Android 11+) and RSA-signature auth (older).
 */
class AdbClient(private val host: String, private val port: Int) : Closeable {

    private lateinit var socket: Socket
    private lateinit var plainIn:  DataInputStream
    private lateinit var plainOut: DataOutputStream

    private var useTls = false
    private lateinit var tlsSocket: SSLSocket
    private lateinit var tlsIn:  DataInputStream
    private lateinit var tlsOut: DataOutputStream

    private val input  get() = if (useTls) tlsIn  else plainIn
    private val output get() = if (useTls) tlsOut else plainOut

    fun connect() {
        socket = Socket(host, port).also { it.tcpNoDelay = true }
        plainIn  = DataInputStream(socket.getInputStream())
        plainOut = DataOutputStream(socket.getOutputStream())

        write(A_CNXN, A_VERSION, A_MAXDATA, "host::")

        var msg = read()

        if (msg.command == A_STLS) {
            // Android 11+ Wireless Debugging uses TLS
            write(A_STLS, A_STLS_VERSION, 0)
            val ssl = AdbKey.sslContext.socketFactory
                .createSocket(socket, host, port, true) as SSLSocket
            ssl.startHandshake()
            Log.d(TAG, "TLS handshake OK")
            tlsSocket = ssl
            tlsIn  = DataInputStream(ssl.inputStream)
            tlsOut = DataOutputStream(ssl.outputStream)
            useTls = true
            msg = read()

        } else if (msg.command == A_AUTH && msg.arg0 == ADB_AUTH_TOKEN) {
            // Older RSA-signature auth
            write(A_AUTH, ADB_AUTH_SIGNATURE, 0, AdbKey.sign(msg.data!!))
            msg = read()
            if (msg.command != A_CNXN) {
                // Device doesn't recognise our key yet → send public key
                write(A_AUTH, ADB_AUTH_RSAPUBLICKEY, 0, AdbKey.adbPublicKey)
                msg = read()
            }
        }

        if (msg.command != A_CNXN) throw AdbException("Expected A_CNXN, got ${msg.command}")
        Log.d(TAG, "ADB connected to $host:$port")
    }

    fun shell(command: String): String {
        val localId = 1
        write(A_OPEN, localId, 0, "shell:$command")

        val sb = StringBuilder()
        var msg = read()
        when (msg.command) {
            A_OKAY -> {
                while (true) {
                    msg = read()
                    val remoteId = msg.arg0
                    when (msg.command) {
                        A_WRTE -> {
                            if (msg.data_length > 0)
                                sb.append(String(msg.data!!))
                            write(A_OKAY, localId, remoteId)
                        }
                        A_CLSE -> {
                            write(A_CLSE, localId, remoteId)
                            break
                        }
                        else -> throw AdbException("Unexpected command ${msg.command}")
                    }
                }
            }
            A_CLSE -> write(A_CLSE, localId, msg.arg0)
            else   -> throw AdbException("Expected A_OKAY, got ${msg.command}")
        }
        return sb.toString()
    }

    // ── Wire I/O ──────────────────────────────────────────────────────────────

    private fun write(command: Int, arg0: Int, arg1: Int, data: ByteArray? = null) =
        write(AdbMessage(command, arg0, arg1, data))

    private fun write(command: Int, arg0: Int, arg1: Int, data: String) =
        write(AdbMessage(command, arg0, arg1, data))

    private fun write(msg: AdbMessage) {
        output.write(msg.toByteArray())
        output.flush()
    }

    private fun read(): AdbMessage {
        val buf = ByteBuffer.allocate(AdbMessage.HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN)
        input.readFully(buf.array(), 0, 24)
        val command    = buf.int
        val arg0       = buf.int
        val arg1       = buf.int
        val dataLength = buf.int
        val checksum   = buf.int
        val magic      = buf.int
        val data = if (dataLength > 0) ByteArray(dataLength).also {
            input.readFully(it, 0, dataLength)
        } else null
        val msg = AdbMessage(command, arg0, arg1, dataLength, checksum, magic, data)
        msg.validateOrThrow()
        return msg
    }

    override fun close() {
        runCatching { plainIn.close() }
        runCatching { plainOut.close() }
        runCatching { socket.close() }
        if (useTls) {
            runCatching { tlsIn.close() }
            runCatching { tlsOut.close() }
            runCatching { tlsSocket.close() }
        }
    }
}
