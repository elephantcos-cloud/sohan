package com.shohan.sohan.adb

/**
 * Represents every possible state of the ADB wireless connection.
 */
sealed class ConnectionState {
    /** No connection has been attempted or the previous one was closed. */
    object Disconnected : ConnectionState()

    /** Currently trying to establish the connection. */
    object Connecting : ConnectionState()

    /**
     * Successfully connected and the ADB session is alive.
     * @param host  The host we are connected to (usually 127.0.0.1).
     * @param port  The wireless ADB port from Developer Options.
     */
    data class Connected(val host: String, val port: Int) : ConnectionState()

    /**
     * Connection attempt failed.
     * @param message A human-readable description of what went wrong.
     */
    data class Error(val message: String) : ConnectionState()
}
