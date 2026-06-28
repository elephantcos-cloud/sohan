package com.shohan.sohan.adb
import java.nio.ByteBuffer
import java.nio.ByteOrder
class AdbMessage(
    val command: Int, val arg0: Int, val arg1: Int,
    val data_length: Int, val data_crc32: Int, val magic: Int,
    val data: ByteArray?
) {
    constructor(command: Int, arg0: Int, arg1: Int, data: String)
        : this(command, arg0, arg1, (data + "\u0000").toByteArray())
    constructor(command: Int, arg0: Int, arg1: Int, data: ByteArray?) : this(
        command, arg0, arg1,
        data?.size ?: 0, crc32(data), command.inv(), data
    )
    fun validate(): Boolean {
        if (command != magic.inv()) return false
        if (data_length != 0 && crc32(data) != data_crc32) return false
        return true
    }
    fun validateOrThrow() { if (!validate()) throw IllegalArgumentException("bad ADB message") }
    fun toByteArray(): ByteArray {
        return ByteBuffer.allocate(HEADER_LENGTH + (data?.size ?: 0)).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            putInt(command); putInt(arg0); putInt(arg1)
            putInt(data_length); putInt(data_crc32); putInt(magic)
            if (data != null) put(data)
        }.array()
    }
    companion object {
        const val HEADER_LENGTH = 24
        fun crc32(data: ByteArray?): Int {
            if (data == null) return 0
            var res = 0
            for (b in data) res += if (b >= 0) b else b + 256
            return res
        }
    }
}
