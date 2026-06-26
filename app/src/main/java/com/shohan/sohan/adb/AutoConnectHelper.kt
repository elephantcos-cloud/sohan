package com.shohan.sohan.adb

/**
 * Reads the Wireless ADB TLS port directly from Android system properties
 * so Sohan can connect to its own device without the user manually entering a port.
 *
 * Android stores the active wireless ADB port in:
 *   service.adb.tls.port   (Android 11+, when Wireless Debugging is on)
 *
 * We access this via reflection because [android.os.SystemProperties] is a
 * hidden/internal class not exposed in the public SDK.
 */
object AutoConnectHelper {

    const val HOST = "127.0.0.1"

    /**
     * Returns the active Wireless ADB port, or 0 if Wireless Debugging is
     * not enabled or the property cannot be read.
     */
    fun getAdbPort(): Int {
        return try {
            val cls = Class.forName("android.os.SystemProperties")
            val get = cls.getDeclaredMethod("get", String::class.java, String::class.java)
            val portStr = get.invoke(null, "service.adb.tls.port", "0") as String
            portStr.toIntOrNull()?.takeIf { it > 0 } ?: 0
        } catch (_: Exception) { 0 }
    }

    /** True when Wireless Debugging is currently enabled on this device. */
    fun isWirelessDebuggingEnabled(): Boolean = getAdbPort() > 0
}
