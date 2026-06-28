package com.shohan.sohan.adb
object AutoConnectHelper {
    const val HOST = "127.0.0.1"
    fun getAdbPort(): Int {
        return try {
            val cls = Class.forName("android.os.SystemProperties")
            val get = cls.getDeclaredMethod("get", String::class.java, String::class.java)
            val portStr = get.invoke(null, "service.adb.tls.port", "0") as String
            portStr.toIntOrNull()?.takeIf { it > 0 } ?: 0
        } catch (_: Exception) { 0 }
    }
    fun isWirelessDebuggingEnabled(): Boolean = getAdbPort() > 0
}
