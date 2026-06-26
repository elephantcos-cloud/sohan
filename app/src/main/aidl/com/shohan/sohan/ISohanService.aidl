// ISohanService.aidl
// Public contract for binding to Sohan's privileged ADB bridge.
// Copy this file into your project at the same package path to use Sohan.
package com.shohan.sohan;

interface ISohanService {

    // ── Info ──────────────────────────────────────────────────────────────────

    // Returns the service interface version. Check this before calling anything.
    int getVersion() = 1;

    // True if the internal ADB TCP session is alive.
    boolean isAdbConnected() = 2;

    // ── Shell execution ───────────────────────────────────────────────────────

    // Runs [command] with ADB shell-user permissions. Returns stdout.
    // Throws SecurityException if calling app is not authorized.
    // Call requestPermission() first if you are not yet authorized.
    String shell(String command) = 3;

    // ── Permission system ─────────────────────────────────────────────────────

    // Returns authorized package names. Sohan-only; others get SecurityException.
    List<String> getAuthorizedPackages() = 4;

    // Sends a permission request to Sohan — it will show a dialog to the user.
    // Call this when shell() throws SecurityException.
    // The user can Allow or Deny. Poll hasPermission() or retry shell() after.
    void requestPermission() = 5;

    // Returns true if the calling app currently has permission.
    boolean hasPermission() = 6;

    // ── Privileged actions ────────────────────────────────────────────────────

    // Force-stops [packageName]. Equivalent to Settings → App → Force Stop.
    void forceStop(String packageName) = 7;

    // Clears the cache of [packageName] only (data is NOT touched).
    // Returns "Success" or an error message.
    String clearCache(String packageName) = 8;

    // Uninstalls [packageName] for the current user.
    String uninstall(String packageName) = 9;
}
