package com.shohan.sohan.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Process
import android.os.RemoteException
import com.shohan.sohan.ISohanService
import com.shohan.sohan.R
import com.shohan.sohan.MainActivity
import com.shohan.sohan.adb.AdbManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking

/**
 * Sohan's privileged foreground service.
 *
 * Keeps the Wireless ADB session alive and exposes [ISohanService] via AIDL
 * so any authorized app can execute shell commands with ADB-shell permissions.
 *
 * External apps bind with:
 *   intent.action  = "com.shohan.sohan.SERVICE"
 *   intent.package = "com.shohan.sohan"
 */
class SohanService : Service() {

    companion object {
        const val SERVICE_VERSION = 2
        private const val NOTIF_ID   = 1001
        private const val CHANNEL_ID = "sohan_bridge"

        const val ACTION_STOP = "com.shohan.sohan.ACTION_STOP"

        @Volatile var isRunning = false
            private set

        /**
         * When an unauthorized app calls requestPermission(), its package name
         * is posted here so the main UI can show a dialog.
         * Null means no pending request.
         */
        private val _pendingPermission = MutableStateFlow<String?>(null)
        val pendingPermission: StateFlow<String?> = _pendingPermission.asStateFlow()

        fun clearPendingPermission() { _pendingPermission.value = null }
    }

    // ── AIDL implementation ───────────────────────────────────────────────────

    private val sohanBinder = object : ISohanService.Stub() {

        override fun getVersion(): Int = SERVICE_VERSION

        override fun isAdbConnected(): Boolean = AdbManager.isConnected

        override fun shell(command: String): String {
            checkPermission()
            ensureConnected()
            return runBlocking {
                AdbManager.shell(command).getOrElse { e ->
                    throw RemoteException("Shell error: ${e.message}")
                }
            }
        }

        override fun getAuthorizedPackages(): List<String> {
            onlySelf()
            return AppPermissionManager.getAuthorizedPackages(applicationContext)
        }

        override fun requestPermission() {
            val pkg = callingPackageName() ?: return
            if (AppPermissionManager.isAllowed(applicationContext, Binder.getCallingUid())) return
            // Post to the StateFlow — SohanRoot will pick it up and show a dialog.
            _pendingPermission.value = pkg
        }

        override fun hasPermission(): Boolean =
            AppPermissionManager.isAllowed(applicationContext, Binder.getCallingUid())

        override fun forceStop(packageName: String) {
            checkPermission()
            runBlocking { AdbManager.shell("am force-stop $packageName") }
        }

        override fun clearCache(packageName: String): String {
            checkPermission()
            return runBlocking {
                PrivilegedActions.clearCache(packageName)
                    .fold(onSuccess = { "Success" }, onFailure = { it.message ?: "Error" })
            }
        }

        override fun uninstall(packageName: String): String {
            checkPermission()
            return runBlocking {
                PrivilegedActions.uninstall(packageName)
                    .fold(onSuccess = { "Success" }, onFailure = { it.message ?: "Error" })
            }
        }

        // ── Helpers ───────────────────────────────────────────────────────────

        private fun checkPermission() {
            val uid = Binder.getCallingUid()
            if (uid == Process.myUid()) return // Sohan itself is always allowed.
            if (!AppPermissionManager.isAllowed(applicationContext, uid)) {
                throw SecurityException(
                    "UID $uid is not authorized. Call requestPermission() first."
                )
            }
        }

        private fun ensureConnected() {
            if (!AdbManager.isConnected)
                throw RemoteException("ADB is not connected. Open Sohan to reconnect.")
        }

        private fun onlySelf() {
            if (Binder.getCallingUid() != Process.myUid())
                throw SecurityException("This method is restricted to Sohan.")
        }

        private fun callingPackageName(): String? {
            val uid  = Binder.getCallingUid()
            val pkgs = applicationContext.packageManager.getPackagesForUid(uid)
            return pkgs?.firstOrNull()
        }
    }

    // ── Service lifecycle ─────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        // Refresh notification text whenever the service is nudged.
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification())
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = sohanBinder

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val ch = NotificationChannel(CHANNEL_ID, "Sohan Bridge", NotificationManager.IMPORTANCE_LOW)
            .apply { setShowBadge(false); description = "Wireless ADB bridge" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun buildNotification(): Notification {
        val openPi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val stopPi = PendingIntent.getService(
            this, 0,
            Intent(this, SohanService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        val statusText = if (AdbManager.isConnected) "ADB connected · bridge active"
                         else "ADB disconnected — tap to reconnect"
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Sohan")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openPi)
            .addAction(Notification.Action.Builder(null, "Stop", stopPi).build())
            .setOngoing(true)
            .build()
    }
}
