package app.universal.paymentlistener

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.service.notification.NotificationListenerService

class GuardianService : Service() {
    companion object {
        private const val CHANNEL = "upl_guardian"
        private const val NOTIF_ID = 1001
        private const val ACTION_FORCE = "app.universal.paymentlistener.FORCE_REBIND"

        fun start(context: Context) {
            val i = Intent(context, GuardianService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i) else context.startService(i)
            } catch (_: Exception) {}
        }

        fun forceRebind(context: Context) {
            val i = Intent(context, GuardianService::class.java).setAction(ACTION_FORCE)
            try {
                if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i) else context.startService(i)
            } catch (_: Exception) {}
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var attempt = 0
    private var lastAttempt = 0L

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, notification("Universal Payment Listener aktif"))
        handler.post(watchdog)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_FORCE) requestNow()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private val watchdog = object : Runnable {
        override fun run() {
            if (ListenerRuntimeState.connected) {
                attempt = 0
            } else {
                val delay = longArrayOf(0, 2_000, 5_000, 10_000, 20_000, 30_000)[attempt.coerceAtMost(5)]
                if (System.currentTimeMillis() - lastAttempt >= delay) {
                    requestNow()
                    attempt = (attempt + 1).coerceAtMost(5)
                }
            }
            handler.postDelayed(this, 2_000)
        }
    }

    private fun requestNow() {
        lastAttempt = System.currentTimeMillis()
        try {
            NotificationListenerService.requestRebind(ComponentName(this, PaymentNotificationListener::class.java))
        } catch (_: Exception) {}
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Listener Guardian", NotificationManager.IMPORTANCE_LOW))
        }
    }

    private fun notification(text: String): Notification {
        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL) else Notification.Builder(this)
        return b.setContentTitle("Universal Payment Listener")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_listener_status)
            .setOngoing(true)
            .build()
    }
}
