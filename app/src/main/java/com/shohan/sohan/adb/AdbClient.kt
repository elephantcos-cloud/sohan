package com.shohan.sohan.adb
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
class AdbClient(private val host: String, private val port: Int) : Closeable {
    private lateinit var socket: Socket
    private lateinit var plainIn: DataInputStream
    private lateinit var plainOut: DataOutputStream
    private var useTls = false
    private var tlsIn: DataInputStream? = null
    private var tlsOut: DataOutputStream? = null
    private val input  get() = if (useTls) tlsIn!!  else plainIn
    private val output get() = if (useTls) tlsOut!! else plainOut
    fun connect() {
        socket = Socket(host, port).also { it.tcpNoDelay = true }
        plainIn  = DataInputStream(socket.inputStream)
        plainOut = DataOutputStream(socket.outputStream)
        write(A_CNXN, A_VERSION, A_MAXDATA, "host::")
        var msg = read()
        if (msg.command == A_STLS) {
            write(A_STLS, A_STLS_VERSION, 0)
            val ssl = AdbKey.sslContext.socketFactory.createSocket(socket, host, port, true) as SSLSocket
            ssl.startHandshake()
            tlsIn  = DataInputStream(ssl.inputStream)
            tlsOut = DataOutputStream(ssl.outputStream)
            useTls = true
            msg = read()
        } else if (msg.command == A_AUTH && msg.arg0 == ADB_AUTH_TOKEN) {
            write(A_AUTH, ADB_AUTH_SIGNATURE, 0, AdbKey.sign(msg.data!!))
            msg = read()
            if (msg.command != A_CNXN) {
                write(A_AUTH, ADB_AUTH_RSAPUBLICKEY, 0, AdbKey.adbPublicKey)
                msg = read()
            }
        }
        if (msg.command != A_CNXN) throw AdbException("Expected A_CNXN got \${msg.command}")
    }
    fun shell(command: String): String {
        val localId = 1
        write(A_OPEN, localId, 0, "shell:\$command")
        val sb = StringBuilder()
        var msg = read()
        if (msg.command == A_OKAY) {
            while (true) {
                msg = read()
                when (msg.command) {
                    A_WRTE -> { if (msg.data_length > 0) sb.append(String(msg.data!!)); write(A_OKAY, localId, msg.arg0) }
                    A_CLSE -> { write(A_CLSE, localId, msg.arg0); break }
                    else -> throw AdbException("Unexpected \${msg.command}")
                }
            }
        }
        return sb.toString()
    }
    private fun write(command: Int, arg0: Int, arg1: Int, data: ByteArray? = null) = write(AdbMessage(command, arg0, arg1, data))
    private fun write(command: Int, arg0: Int, arg1: Int, data: String) = write(AdbMessage(command, arg0, arg1, data))
    private fun write(msg: AdbMessage) { output.write(msg.toByteArray()); output.flush() }
    private fun read(): AdbMessage {
        val buf = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
        input.readFully(buf.array(), 0, 24)
        val cmd = buf.int; val a0 = buf.int; val a1 = buf.int
        val dLen = buf.int; val crc = buf.int; val magic = buf.int
        val data = if (dLen > 0) ByteArray(dLen).also { input.readFully(it, 0, dLen) } else null
        return AdbMessage(cmd, a0, a1, dLen, crc, magic, data).also { it.validateOrThrow() }
    }
    override fun close() {
        runCatching { plainIn.close() }; runCatching { plainOut.close() }; runCatching { socket.close() }
        tlsIn?.let { runCatching { it.close() } }; tlsOut?.let { runCatching { it.close() } }
    }
}
