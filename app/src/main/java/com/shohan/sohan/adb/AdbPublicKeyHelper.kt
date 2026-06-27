package com.shohan.sohan.adb

import android.util.Base64
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.interfaces.RSAPublicKey

/**
 * Converts RSA-2048 public key → Android ADB binary format → base64 string.
 * This is the format stored in /data/misc/adb/adb_keys.
 */
internal object AdbPublicKeyHelper {
    private const val WORDS = 64   // 2048 / 32

    fun toAdbPublicKeyBytes(pub: RSAPublicKey, label: String = "sohan"): ByteArray {
        val n    = pub.modulus
        val e    = pub.publicExponent.toInt()
        val R    = BigInteger.ONE.shiftLeft(32 * WORDS)
        val rr   = R.multiply(R).mod(n)
        val n0   = n.toLong() and 0xFFFFFFFFL
        val n0inv = montInv32(n0)
        val buf  = ByteBuffer.allocate(4 + 4 + 4 * WORDS + 4 * WORDS + 4)
            .order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(WORDS)
        buf.putInt(n0inv.toInt())
        toLE32(n).forEach  { buf.putInt(it) }
        toLE32(rr).forEach { buf.putInt(it) }
        buf.putInt(e)
        val b64 = Base64.encodeToString(buf.array(), Base64.NO_WRAP)
        return "$b64 $label\n".toByteArray(Charsets.UTF_8)
    }

    private fun toLE32(n: BigInteger): IntArray {
        val a = IntArray(WORDS); var t = n
        for (i in 0 until WORDS) {
            a[i] = t.and(BigInteger.valueOf(0xFFFFFFFFL)).toInt()
            t = t.shiftRight(32)
        }
        return a
    }

    private fun montInv32(n0: Long): Long {
        var x = n0
        repeat(5) { x = x * (2L - n0 * x) }
        return (-x) and 0xFFFFFFFFL
    }
}
