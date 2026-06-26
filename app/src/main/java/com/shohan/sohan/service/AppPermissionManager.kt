package com.shohan.sohan.service

import android.content.Context

/**
 * Manages which external apps are allowed to call into [SohanService].
 *
 * Permissions are stored in a private SharedPreferences file so they
 * survive service restarts. Each entry is keyed by package name with a
 * Boolean value (true = allowed, absent/false = denied).
 */
object AppPermissionManager {

    private const val PREFS = "sohan_app_permissions"

    // ── Permission checks ─────────────────────────────────────────────────────

    /**
     * Returns true if at least one package owned by [uid] has been granted
     * permission by the user.
     */
    fun isAllowed(context: Context, uid: Int): Boolean {
        val pm     = context.packageManager
        val prefs  = prefs(context)
        val pkgs   = pm.getPackagesForUid(uid) ?: return false
        return pkgs.any { prefs.getBoolean(it, false) }
    }

    // ── Grant / revoke ────────────────────────────────────────────────────────

    fun grant(context: Context, packageName: String) {
        prefs(context).edit().putBoolean(packageName, true).apply()
    }

    fun revoke(context: Context, packageName: String) {
        prefs(context).edit().remove(packageName).apply()
    }

    // ── Listing ───────────────────────────────────────────────────────────────

    /**
     * Returns every package name the user has authorized.
     */
    fun getAuthorizedPackages(context: Context): List<String> =
        prefs(context).all
            .filter { it.value == true }
            .map { it.key }
            .sorted()

    /**
     * Returns true if [packageName] is currently in the allowed list.
     */
    fun isPackageAllowed(context: Context, packageName: String): Boolean =
        prefs(context).getBoolean(packageName, false)

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
