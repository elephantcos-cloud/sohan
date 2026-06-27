package com.shohan.sohan.adb

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.math.BigInteger
import java.net.Socket
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import java.util.Date
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.X509ExtendedTrustManager
import javax.security.auth.x500.X500Principal

/**
 * Manages the RSA-2048 key pair stored in Android Keystore.
 * No BouncyCastle needed — the Keystore itself generates the self-signed cert.
 */
object AdbKey {

    private const val ALIAS    = "sohan_adb_key"
    private const val PROVIDER = "AndroidKeyStore"

    /** Ensure the key exists; generate if absent. */
    fun ensureKey() {
        val ks = KeyStore.getInstance(PROVIDER).also { it.load(null) }
        if (!ks.containsAlias(ALIAS)) generate()
    }

    private fun generate() {
        val kpg  = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setKeySize(2048)
            .setAlgorithmParameterSpec(
                java.security.spec.RSAKeyGenParameterSpec(2048, java.security.spec.RSAKeyGenParameterSpec.F4)
            )
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .setDigests(
                KeyProperties.DIGEST_SHA1,
                KeyProperties.DIGEST_SHA256,
                KeyProperties.DIGEST_SHA512
            )
            .setCertificateSubject(X500Principal("CN=sohan"))
            .setCertificateSerialNumber(BigInteger.ONE)
            .setCertificateNotBefore(Date(System.currentTimeMillis() - 86_400_000L))
            .setCertificateNotAfter(Date(System.currentTimeMillis() + 315_360_000_000L)) // 10 years
            .build()
        kpg.initialize(spec)
        kpg.generateKeyPair()
    }

    /** Sign ADB auth token with our private key. */
    fun sign(data: ByteArray): ByteArray {
        val ks  = KeyStore.getInstance(PROVIDER).also { it.load(null) }
        val priv = ks.getKey(ALIAS, null) as PrivateKey
        return Signature.getInstance("SHA1withRSA").run {
            initSign(priv)
            update(data)
            sign()
        }
    }

    /** RSA public key in Android ADB binary format (for A_AUTH_RSAPUBLICKEY). */
    val adbPublicKey: ByteArray
        get() {
            val ks   = KeyStore.getInstance(PROVIDER).also { it.load(null) }
            val cert = ks.getCertificate(ALIAS)
            return AdbPublicKeyHelper.toAdbPublicKeyBytes(cert.publicKey as RSAPublicKey)
        }

    /**
     * SSLContext with our key as the client certificate.
     * adbd expects mutual TLS on Android 11+ Wireless Debugging.
     */
    val sslContext: SSLContext
        get() {
            val ks  = KeyStore.getInstance(PROVIDER).also { it.load(null) }
            val kmf = KeyManagerFactory.getInstance("X509")
            kmf.init(ks, null)
            return SSLContext.getInstance("TLS").also { ctx ->
                ctx.init(kmf.keyManagers, arrayOf(TrustAllManager()), null)
            }
        }

    /** Trust manager that accepts any server certificate (adbd is self-signed). */
    private class TrustAllManager : X509ExtendedTrustManager() {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, auth: String?, socket: Socket?) = Unit
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, auth: String?, socket: Socket?) = Unit
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, auth: String?, engine: SSLEngine?) = Unit
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, auth: String?, engine: SSLEngine?) = Unit
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, auth: String?) = Unit
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, auth: String?) = Unit
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }
}
