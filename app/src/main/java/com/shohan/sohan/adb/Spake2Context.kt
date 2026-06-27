package com.shohan.sohan.adb

import org.bouncycastle.asn1.x9.ECNamedCurveTable
import org.bouncycastle.math.ec.ECPoint
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

internal class Spake2Context(private val password: ByteArray) {
    private val spec  = ECNamedCurveTable.getParameterSpec("prime256v1")
    private val curve = spec.curve
    private val G     = spec.g
    private val n     = spec.n
    private val M: ECPoint = curve.decodePoint(M_BYTES)
    private val N: ECPoint = curve.decodePoint(N_BYTES)
    private val x: BigInteger = BigInteger(n.bitLength(), SecureRandom()).mod(n)
    private val w: BigInteger = BigInteger(1, sha256(password)).mod(n)
    val msg: ByteArray = G.multiply(x).add(M.multiply(w)).normalize().getEncoded(false)
    private var aesKey: ByteArray? = null
    private var encCount = 0
    private var decCount = 0

    fun initCipher(theirMsg: ByteArray): Boolean = try {
        val S  = curve.decodePoint(theirMsg)
        val K  = S.subtract(N.multiply(w)).multiply(x).normalize()
        val Kb = K.getEncoded(false)
        val digest = MessageDigest.getInstance("SHA-256").apply {
            update(msg); update(theirMsg); update(Kb); update(password)
        }
        aesKey = digest.digest().copyOf(16)
        true
    } catch (_: Exception) { false }

    fun encrypt(data: ByteArray): ByteArray? = aesgcm(Cipher.ENCRYPT_MODE, data, encCount++)
    fun decrypt(data: ByteArray): ByteArray? = aesgcm(Cipher.DECRYPT_MODE, data, decCount++)

    private fun aesgcm(mode: Int, data: ByteArray, cnt: Int): ByteArray? {
        val key = aesKey ?: return null
        return try {
            val nonce = ByteArray(12).also {
                it[0] = (cnt        and 0xFF).toByte()
                it[1] = (cnt shr  8 and 0xFF).toByte()
                it[2] = (cnt shr 16 and 0xFF).toByte()
                it[3] = (cnt shr 24 and 0xFF).toByte()
            }
            Cipher.getInstance("AES/GCM/NoPadding").also {
                it.init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
            }.doFinal(data)
        } catch (_: Exception) { null }
    }

    companion object {
        private val M_BYTES = hex("02886e2f97ace46e55ba9dd7242579f2993b64e16ef3dcab95afd497333d8fa12f")
        private val N_BYTES = hex("03d8bbd6c639c62937b04d997f38c3770719c629d7014d49a24b4f98baa1292b49")
        private fun hex(s: String) = ByteArray(s.length/2) { s.substring(it*2,it*2+2).toInt(16).toByte() }
        private fun sha256(b: ByteArray) = MessageDigest.getInstance("SHA-256").digest(b)
    }
}
