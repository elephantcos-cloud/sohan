package com.shohan.sohan.adb
import android.util.Base64
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.interfaces.RSAPublicKey
internal object AdbPublicKeyHelper {
    private const val WORDS = 64
    fun toAdbPublicKeyBytes(pub: RSAPublicKey, label: String = "sohan"): ByteArray {
        val n = pub.modulus; val e = pub.publicExponent.toInt()
        val R = BigInteger.ONE.shiftLeft(32 * WORDS)
        val rr = R.multiply(R).mod(n)
        val n0 = n.toLong() and 0xFFFFFFFFL
        var x = n0; repeat(5) { x = x * (2L - n0 * x) }
        val n0inv = (-x) and 0xFFFFFFFFL
        val buf = ByteBuffer.allocate(4 + 4 + 4*WORDS + 4*WORDS + 4).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(WORDS); buf.putInt(n0inv.toInt())
        fun le32(v: BigInteger): IntArray { val a = IntArray(WORDS); var t = v
            for (i in 0 until WORDS) { a[i] = t.and(BigInteger.valueOf(0xFFFFFFFFL)).toInt(); t = t.shiftRight(32) }; return a }
        le32(n).forEach { buf.putInt(it) }; le32(rr).forEach { buf.putInt(it) }
        buf.putInt(e)
        return (Base64.encodeToString(buf.array(), Base64.NO_WRAP) + " " + label + "\n").toByteArray(Charsets.UTF_8)
    }
}
