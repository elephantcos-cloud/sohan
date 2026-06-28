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
import java.security.spec.RSAKeyGenParameterSpec
import java.util.Date
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.X509ExtendedTrustManager
import javax.security.auth.x500.X500Principal
object AdbKey {
    private const val ALIAS    = "sohan_adb_key"
    private const val PROVIDER = "AndroidKeyStore"
    fun ensureKey() {
        val ks = KeyStore.getInstance(PROVIDER).also { it.load(null) }
        if (!ks.containsAlias(ALIAS)) generate()
    }
    private fun generate() {
        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, PROVIDER)
        kpg.initialize(KeyGenParameterSpec.Builder(ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
            .setKeySize(2048)
            .setAlgorithmParameterSpec(RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4))
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .setDigests(KeyProperties.DIGEST_SHA1, KeyProperties.DIGEST_SHA256)
            .setCertificateSubject(X500Principal("CN=sohan"))
            .setCertificateSerialNumber(BigInteger.ONE)
            .setCertificateNotBefore(Date(System.currentTimeMillis() - 86_400_000L))
            .setCertificateNotAfter(Date(System.currentTimeMillis() + 315_360_000_000L))
            .build())
        kpg.generateKeyPair()
    }
    fun sign(data: ByteArray): ByteArray {
        val ks = KeyStore.getInstance(PROVIDER).also { it.load(null) }
        val priv = ks.getKey(ALIAS, null) as PrivateKey
        return Signature.getInstance("SHA1withRSA").run { initSign(priv); update(data); sign() }
    }
    val adbPublicKey: ByteArray get() {
        val ks = KeyStore.getInstance(PROVIDER).also { it.load(null) }
        return AdbPublicKeyHelper.toAdbPublicKeyBytes(ks.getCertificate(ALIAS).publicKey as RSAPublicKey)
    }
    val sslContext: SSLContext get() {
        val ks = KeyStore.getInstance(PROVIDER).also { it.load(null) }
        val kmf = KeyManagerFactory.getInstance("X509").also { it.init(ks, null) }
        return SSLContext.getInstance("TLS").also { it.init(kmf.keyManagers, arrayOf(TrustAll()), null) }
    }
    private class TrustAll : X509ExtendedTrustManager() {
        override fun checkClientTrusted(c: Array<out X509Certificate>?, a: String?, s: Socket?) = Unit
        override fun checkServerTrusted(c: Array<out X509Certificate>?, a: String?, s: Socket?) = Unit
        override fun checkClientTrusted(c: Array<out X509Certificate>?, a: String?, e: SSLEngine?) = Unit
        override fun checkServerTrusted(c: Array<out X509Certificate>?, a: String?, e: SSLEngine?) = Unit
        override fun checkClientTrusted(c: Array<out X509Certificate>?, a: String?) = Unit
        override fun checkServerTrusted(c: Array<out X509Certificate>?, a: String?) = Unit
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }
}
