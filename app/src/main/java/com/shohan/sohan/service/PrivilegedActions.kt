package com.shohan.sohan.service

import android.os.Build
import com.shohan.sohan.adb.AdbManager

/**
 * High-level privileged actions that run through the active ADB session.
 *
 * All functions are suspend and must be called from a coroutine.
 * They return [Result] so callers can handle failure gracefully.
 */
object PrivilegedActions {

    // ── App lifecycle ─────────────────────────────────────────────────────────

    /**
     * Force-stops [packageName].
     * Equivalent to Settings → App → Force Stop.
     */
    suspend fun forceStop(packageName: String): Result<Unit> =
        AdbManager.shell("am force-stop $packageName").map { }

    /**
     * Starts [packageName]'s main launch activity (if it has one).
     */
    suspend fun launchApp(packageName: String): Result<Unit> =
        AdbManager.shell(
            "monkey -p $packageName -c android.intent.category.LAUNCHER 1"
        ).map { }

    // ── Cache management ──────────────────────────────────────────────────────

    /**
     * Clears only the cache of [packageName] — user data is untouched.
     *
     * On Android 8+: uses `pm clear --cache-only` (data-safe).
     * Below Android 8: falls back to `pm clear` (clears everything — warn user).
     */
    suspend fun clearCache(packageName: String): Result<Unit> {
        val cmd = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            "pm clear --cache-only $packageName"
        } else {
            "pm clear $packageName"
        }
        val result = AdbManager.shell(cmd)
        return result.flatMap { output ->
            if (output.contains("Success", ignoreCase = true)) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("pm clear failed for $packageName: $output"))
            }
        }
    }

    /**
     * Clears cache for every package in [packages].
     * @return Pair(successCount, failedPackages)
     */
    suspend fun clearCacheBulk(packages: List<String>): Pair<Int, List<String>> {
        var ok = 0
        val failed = mutableListOf<String>()
        packages.forEach { pkg ->
            if (clearCache(pkg).isSuccess) ok++ else failed.add(pkg)
        }
        return ok to failed
    }

    // ── Package management ────────────────────────────────────────────────────

    /**
     * Returns a list of all installed package names.
     * @param userOnly if true, only user-installed (third-party) packages.
     */
    suspend fun listPackages(userOnly: Boolean = false): Result<List<String>> {
        val cmd = if (userOnly) "pm list packages -3" else "pm list packages"
        return AdbManager.shell(cmd).map { output ->
            output.lines()
                .filter { it.startsWith("package:") }
                .map { it.removePrefix("package:").trim() }
                .filter { it.isNotEmpty() }
        }
    }

    /**
     * Uninstalls [packageName] for the current user.
     * System apps will fail; user apps succeed.
     */
    suspend fun uninstall(packageName: String): Result<Unit> =
        AdbManager.shell("pm uninstall $packageName").flatMap { output ->
            if (output.contains("Success", ignoreCase = true))
                Result.success(Unit)
            else
                Result.failure(Exception("Uninstall failed: $output"))
        }

    /**
     * Installs an APK from [apkPath] on the device's filesystem.
     */
    suspend fun install(apkPath: String): Result<Unit> =
        AdbManager.shell("pm install -r $apkPath").flatMap { output ->
            if (output.contains("Success", ignoreCase = true))
                Result.success(Unit)
            else
                Result.failure(Exception("Install failed: $output"))
        }

    /**
     * Grants a runtime permission to [packageName].
     * Useful for automating permission grants.
     */
    suspend fun grantRuntimePermission(
        packageName: String,
        permission: String
    ): Result<Unit> =
        AdbManager.shell("pm grant $packageName $permission").map { }

    // ── System info ───────────────────────────────────────────────────────────

    /** Returns raw battery stats from dumpsys. */
    suspend fun getBatteryInfo(): Result<String> =
        AdbManager.shell("dumpsys battery")

    /** Returns /proc/meminfo. */
    suspend fun getMemoryInfo(): Result<String> =
        AdbManager.shell("cat /proc/meminfo")

    /** Returns disk usage of /data partition. */
    suspend fun getDiskInfo(): Result<String> =
        AdbManager.shell("df /data")

    /** Returns currently running processes. */
    suspend fun getRunningApps(): Result<List<String>> =
        AdbManager.shell("am dump-heap -n 0 2>/dev/null; dumpsys activity processes | grep 'ProcessRecord'")
            .map { it.lines().filter { l -> l.contains("ProcessRecord") } }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> =
        fold(onSuccess = transform, onFailure = { Result.failure(it) })
}
