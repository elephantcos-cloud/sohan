package com.shohan.sohan.client

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Helper to check if Sohan is installed and open its install/Play page if not.
 */
object SohanInstallChecker {

    private const val SOHAN_PACKAGE = "com.shohan.sohan"

    /**
     * Returns true if the Sohan app is installed on this device.
     */
    fun isInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(SOHAN_PACKAGE, 0)
            true
        } catch (_: Exception) { false }
    }

    /**
     * Opens the Sohan GitHub releases page so the user can download and install it.
     */
    fun openInstallPage(context: Context) {
        val uri = Uri.parse("https://github.com/elephantcos-cloud/sohan/releases")
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    /**
     * Opens the Sohan app if it is installed.
     * Useful to prompt the user to connect ADB before using your app.
     */
    fun openSohan(context: Context): Boolean {
        val intent = context.packageManager
            .getLaunchIntentForPackage(SOHAN_PACKAGE) ?: return false
        context.startActivity(intent)
        return true
    }
}
