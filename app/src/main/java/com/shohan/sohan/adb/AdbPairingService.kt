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
@RequiresApi(Build.VERSION_CODES.R)
class AdbPairingService : Service() {
    companion object { private const val CHANNEL_ID = "sohan_watcher"; private const val NOTIF_ID = 3001 }
    private var mdns: AdbMdns? = null
    private val portObserver = Observer<Int> { port -> if (port != null && port > 0) notifyReady(port) else notifyWaiting() }
    override fun onCreate() {
        super.onCreate(); createChannel(); startForeground(NOTIF_ID, buildWaiting())
        mdns = AdbMdns(this, AdbMdns.TLS_CONNECT, portObserver).also { it.start() }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
    override fun onDestroy() { super.onDestroy(); mdns?.stop() }
    override fun onBind(intent: Intent?): IBinder? = null
    private fun createChannel() {
        val ch = NotificationChannel(CHANNEL_ID, "Wireless ADB Watcher", NotificationManager.IMPORTANCE_LOW)
            .apply { setShowBadge(false); setSound(null, null) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }
    private fun openAppPi() = PendingIntent.getActivity(this, 0,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    private fun buildWaiting() = Notification.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification).setContentTitle("Sohan — Waiting")
        .setContentText("Developer Options → Wireless Debugging চালু করো")
        .setContentIntent(openAppPi()).setOngoing(true).build()
    private fun buildReady(port: Int) = Notification.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification).setContentTitle("Wireless Debugging চালু আছে")
        .setContentText("Tap করো Sohan connect করতে (port \$port)")
        .setContentIntent(openAppPi())
        .addAction(Notification.Action.Builder(null, "Connect Now", openAppPi()).build())
        .setColor(0xFF00C853.toInt()).setColorized(true).setOngoing(true).build()
    private fun notifyWaiting() = getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildWaiting())
    private fun notifyReady(port: Int) = getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildReady(port))
}
