package com.shohan.sohan.client

/** SDK version constants. */
object SohanVersion {
    /** Current SDK version. */
    const val SDK_VERSION = 1

    /**
     * Minimum Sohan app version this SDK is compatible with.
     * If [SohanClient.connect] returns a version lower than this,
     * warn the user to update Sohan.
     */
    const val MIN_SERVICE_VERSION = 2
}
