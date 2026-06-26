package com.shohan.sohan.data

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings

/**
 * Checks and opens system permission screens that Sohan needs but that
 * cannot be granted via a runtime permission request dialog.
 */
object PermissionHelper {

    // ── Usage Access (PACKAGE_USAGE_STATS) ────────────────────────────────────

    /**
     * Returns true if the user has granted Usage Access to Sohan.
     * Required by [AppListRepository] to read per-app storage stats.
     */
    fun hasUsageAccess(context: Context): Boolean {
        return try {
            val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = ops.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) { false }
    }

    /**
     * Opens Settings → Usage access so the user can grant it.
     */
    fun openUsageAccessSettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    // ── Wireless Debugging (Developer Options) ────────────────────────────────

    /**
     * Opens Developer Options (best effort — no direct intent for Wireless
     * Debugging on all devices, so we open Developer Options instead).
     */
    fun openDeveloperOptions(context: Context) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            )
        } catch (_: Exception) {
            context.startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }
}
