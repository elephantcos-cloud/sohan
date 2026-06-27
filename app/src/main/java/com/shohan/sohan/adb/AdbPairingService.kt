package com.shohan.sohan.adb

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.lifecycle.Observer
import com.shohan.sohan.MainActivity
import com.shohan.sohan.R

/**
 * Foreground service that watches for Wireless Debugging via mDNS.
 *
 * Uses AdbMdns to listen for _adb-tls-connect._tcp (the regular connect port).
 * When the port appears → shows "Wireless Debugging is ON — tap to connect".
 * Tapping opens MainActivity which auto-connects via AutoConnectHelper.
 *
 * For one-time pairing: user just taps "Allow" on the system dialog that
 * appears on the first ADB connection — no pairing code needed for self-connect.
 */
@RequiresApi(Build.VERSION_CODES.R)
class AdbPairingService : Service() {

    companion object {
        private const val CHANNEL_ID = "sohan_watcher"
        private const val NOTIF_ID   = 3001
    }

    private var mdns: AdbMdns? = null

    private val portObserver = Observer<Int> { port ->
        if (port != null && port > 0) {
            notifyReady(port)
        } else {
            notifyWaiting()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildWaiting())

        mdns = AdbMdns(this, AdbMdns.TLS_CONNECT, portObserver).also { it.start() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onDestroy() {
        super.onDestroy()
        mdns?.stop()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Notifications ─────────────────────────────────────────────────────────

    private fun createChannel() {
        val ch = NotificationChannel(
            CHANNEL_ID, "Wireless ADB Watcher", NotificationManager.IMPORTANCE_LOW
        ).apply { setShowBadge(false); setSound(null, null) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    private fun openAppIntent() = PendingIntent.getActivity(
        this, 0,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun buildWaiting() = Notification.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("Sohan — Waiting")
        .setContentText("Enable Wireless Debugging in Developer Options")
        .setContentIntent(openAppIntent())
        .setOngoing(true)
        .build()

    private fun buildReady(port: Int) = Notification.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("Wireless Debugging is ON")
        .setContentText("Tap to connect Sohan (port $port)")
        .setContentIntent(openAppIntent())
        .addAction(
            Notification.Action.Builder(null, "⚡ Connect Now", openAppIntent()).build()
        )
        .setColor(0xFF00C853.toInt()).setColorized(true)
        .setOngoing(true)
        .build()

    private fun notifyWaiting() {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ID, buildWaiting())
    }

    private fun notifyReady(port: Int) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIF_ID, buildReady(port))
    }
}
