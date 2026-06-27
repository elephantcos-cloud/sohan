package com.shohan.sohan.adb

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.RemoteInput
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import com.shohan.sohan.MainActivity
import com.shohan.sohan.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class AdbPairingService : Service() {
    companion object {
        private const val CHANNEL_ID  = "sohan_pairing"
        private const val NOTIF_WAIT  = 3001
        private const val NOTIF_READY = 3002
        private const val NOTIF_BUSY  = 3003
        const val ACTION_SUBMIT = "com.shohan.sohan.ACTION_SUBMIT_PAIR_CODE"
        const val RI_KEY        = "pair_code_input"
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var watchJob: Job? = null
    private var curHost: String? = null
    private var curPort: Int     = 0

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action != ACTION_SUBMIT) return
            val code = RemoteInput.getResultsFromIntent(intent)
                ?.getCharSequence(RI_KEY)?.toString()?.trim() ?: return
            val h = curHost ?: return
            val p = curPort.takeIf { it > 0 } ?: return
            doPair(h, p, code)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_WAIT, waitingNotif())
        registerReceiver(receiver, IntentFilter(ACTION_SUBMIT), RECEIVER_NOT_EXPORTED)
        watchJob = AdbMdns.pairingServiceFlow(this).onEach { info ->
            if (info == null) {
                curHost = null; curPort = 0
                nm().cancel(NOTIF_READY)
                nm().notify(NOTIF_WAIT, waitingNotif())
            } else {
                curHost = info.host; curPort = info.port
                nm().cancel(NOTIF_WAIT)
                nm().notify(NOTIF_READY, readyNotif(info.port))
            }
        }.launchIn(scope)
    }

    override fun onStartCommand(i: Intent?, f: Int, s: Int) = START_STICKY
    override fun onDestroy() { super.onDestroy(); unregisterReceiver(receiver); watchJob?.cancel() }
    override fun onBind(i: Intent?): IBinder? = null

    private fun doPair(host: String, port: Int, code: String) {
        nm().cancel(NOTIF_READY)
        nm().notify(NOTIF_BUSY, pairingNotif())
        scope.launch {
            try {
                AdbPairingClient(host, port, code, applicationContext).pair()
                nm().cancel(NOTIF_BUSY)
                nm().notify(NOTIF_BUSY, successNotif())
                nm().notify(NOTIF_WAIT, waitingNotif())
            } catch (e: AdbPairingClient.InvalidPairCodeException) {
                nm().cancel(NOTIF_BUSY)
                nm().notify(NOTIF_BUSY, errorNotif("Wrong code — try again"))
                nm().notify(NOTIF_READY, readyNotif(port))
            } catch (e: Exception) {
                nm().cancel(NOTIF_BUSY)
                nm().notify(NOTIF_BUSY, errorNotif(e.message ?: "Pairing failed"))
            }
        }
    }

    private fun createChannel() {
        nm().createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "ADB Pairing", NotificationManager.IMPORTANCE_HIGH)
                .apply { setShowBadge(false) }
        )
    }

    private fun waitingNotif() = Notification.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("Sohan — Waiting for pairing")
        .setContentText("Developer Options → Wireless Debugging → Pair device with pairing code")
        .setOngoing(true).build()

    private fun readyNotif(port: Int): Notification {
        val ri  = RemoteInput.Builder(RI_KEY).setLabel("6-digit pairing code").build()
        val pi  = PendingIntent.getBroadcast(this, 0,
            Intent(ACTION_SUBMIT).setPackage(packageName),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val act = Notification.Action.Builder(null, "Pair now", pi).addRemoteInput(ri).build()
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("\uD83D\uDD17 Wireless Debugging ready to pair")
            .setContentText("Enter the 6-digit code (port \${port})")
            .addAction(act).setOngoing(true)
            .setColor(0xFF2979FF.toInt()).setColorized(true).build()
    }

    private fun pairingNotif() = Notification.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification).setContentTitle("Pairing\u2026")
        .setProgress(0,0,true).setOngoing(true).build()

    private fun successNotif(): Notification {
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL_ID).setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("\u2705 Pairing successful!")
            .setContentText("Sohan will now auto-connect. Tap to open.")
            .setContentIntent(pi).setAutoCancel(true)
            .setColor(0xFF00C853.toInt()).setColorized(true).build()
    }

    private fun errorNotif(msg: String) = Notification.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification).setContentTitle("\u274C Pairing failed")
        .setContentText(msg).setAutoCancel(true).build()

    private fun nm() = getSystemService(NotificationManager::class.java)
}
