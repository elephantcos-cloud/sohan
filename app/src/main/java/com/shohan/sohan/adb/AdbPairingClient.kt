package com.shohan.sohan.adb

import android.content.Context
import java.io.DataInputStream
import java.io.DataOutputStream
import java.math.BigInteger
import java.net.Socket
import java.security.KeyFactory
import java.security.KeyStore
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAKeyGenParameterSpec
import java.security.spec.RSAPublicKeySpec
import javax.crypto.Cipher
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class AdbPairingClient(
    private val host:     String,
    private val port:     Int,
    private val pairCode: String,
    private val context:  Context
) {
    class InvalidPairCodeException : Exception("Invalid pairing code")
    class PairingFailedException(msg: String) : Exception(msg)

    private val VERSION:   Byte = 1
    private val T_SPAKE2:  Byte = 0
    private val T_PEER:    Byte = 1
    private val PEER_SIZE = 8192
    private val TLS_LABEL = "adb-label\u0000"
    private val TLS_KEY_LEN = 64

    fun pair() {
        val raw = Socket(host, port).also { it.tcpNoDelay = true }
        val sslCtx = SSLContext.getInstance("TLS").also { ctx ->
            ctx.init(null, arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(c: Array<out java.security.cert.X509Certificate>?, a: String?) = Unit
                override fun checkServerTrusted(c: Array<out java.security.cert.X509Certificate>?, a: String?) = Unit
                override fun getAcceptedIssuers() = arrayOf<java.security.cert.X509Certificate>()
            }), null)
        }
        val ssl = sslCtx.socketFactory.createSocket(raw, host, port, true) as SSLSocket
        ssl.startHandshake()
        val inp = DataInputStream(ssl.inputStream)
        val out = DataOutputStream(ssl.outputStream)

        val km       = exportKeyMaterial(ssl)
        val password = pairCode.toByteArray(Charsets.UTF_8) + km
        val spake2   = Spake2Context(password)

        sendPacket(out, T_SPAKE2, spake2.msg)
        val theirSpake = readPacket(inp)
        if (!spake2.initCipher(theirSpake))
            throw PairingFailedException("SPAKE2 cipher init failed")

        val enc = spake2.encrypt(buildPeerInfo())
            ?: throw PairingFailedException("Encrypt failed")
        sendPacket(out, T_PEER, enc)

        val theirEnc  = readPacket(inp)
        val theirPeer = spake2.decrypt(theirEnc) ?: throw InvalidPairCodeException()
        if (theirPeer.isEmpty() || theirPeer[0] != 0.toByte())
            throw PairingFailedException("Bad peer type")
        ssl.close()
    }

    private fun sendPacket(out: DataOutputStream, type: Byte, payload: ByteArray) {
        out.writeByte(VERSION.toInt()); out.writeByte(type.toInt())
        out.writeInt(payload.size); out.write(payload); out.flush()
    }
    private fun readPacket(inp: DataInputStream): ByteArray {
        inp.readByte(); inp.readByte()
        val len = inp.readInt()
        return ByteArray(len).also { inp.readFully(it) }
    }

    private fun buildPeerInfo(): ByteArray {
        val pub    = loadPublicKey()
        val adbKey = AdbPublicKeyHelper.toAdbPublicKeyBytes(pub)
        val buf    = ByteArray(PEER_SIZE)
        buf[0] = 0
        adbKey.copyInto(buf, 1, 0, minOf(adbKey.size, PEER_SIZE - 1))
        return buf
    }

    private fun loadPublicKey(): RSAPublicKey {
        val prefs = context.getSharedPreferences("sohan_adb_secure", Context.MODE_PRIVATE)
        val ks    = KeyStore.getInstance("AndroidKeyStore").also { it.load(null) }
        val aesKey = ks.getKey("sohan_adb_aes_key", null)
            ?: throw PairingFailedException("Connect once before pairing")
        val b64   = prefs.getString("enc_rsa_private", null)
            ?: throw PairingFailedException("Connect once before pairing")
        val blob  = android.util.Base64.decode(b64, android.util.Base64.NO_WRAP)
        val iv    = blob.copyOfRange(0, 12)
        val ct    = blob.copyOfRange(12, blob.size)
        val priv  = Cipher.getInstance("AES/GCM/NoPadding").also {
            it.init(Cipher.DECRYPT_MODE, aesKey, javax.crypto.spec.GCMParameterSpec(128, iv))
        }.doFinal(ct)
        val pk = KeyFactory.getInstance("RSA")
            .generatePrivate(PKCS8EncodedKeySpec(priv)) as RSAPrivateKey
        return KeyFactory.getInstance("RSA")
            .generatePublic(RSAPublicKeySpec(pk.modulus, RSAKeyGenParameterSpec.F4)) as RSAPublicKey
    }

    private fun exportKeyMaterial(ssl: SSLSocket): ByteArray = try {
        val cls = Class.forName("com.android.org.conscrypt.Conscrypt")
        val m   = cls.getMethod("exportKeyingMaterial",
            SSLSocket::class.java, String::class.java, ByteArray::class.java, Int::class.java)
        m.invoke(null, ssl, TLS_LABEL, null, TLS_KEY_LEN) as ByteArray
    } catch (e: Exception) {
        throw PairingFailedException("TLS key export failed: \${e.message}")
    }
}
