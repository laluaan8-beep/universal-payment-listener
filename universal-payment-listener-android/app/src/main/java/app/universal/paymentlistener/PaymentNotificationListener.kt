package app.universal.paymentlistener

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import org.json.JSONObject
import java.security.MessageDigest

class PaymentNotificationListener : NotificationListenerService() {
    companion object {
        fun hasAccess(context: Context): Boolean {
            val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
            return enabled.contains(ComponentName(context, PaymentNotificationListener::class.java).flattenToString())
        }
    }

    override fun onListenerConnected() {
        ListenerRuntimeState.connected = true
        GuardianService.start(this)
        activeNotifications?.takeLast(50)?.forEach { handle(it, true) }
        WorkManager.getInstance(this).enqueue(OneTimeWorkRequestBuilder<HeartbeatWorker>().build())
    }

    override fun onListenerDisconnected() {
        ListenerRuntimeState.connected = false
        GuardianService.forceRebind(this)
        WorkManager.getInstance(this).enqueue(OneTimeWorkRequestBuilder<HeartbeatWorker>().build())
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn != null) handle(sbn, false)
    }

    private fun handle(sbn: StatusBarNotification, recovery: Boolean) {
        val cfg = SecureStore.load(this) ?: return
        if (cfg.allowedPackages.isNotEmpty() && sbn.packageName !in cfg.allowedPackages) return

        val x = NotificationTextExtractor.extract(this, sbn.notification)
        ListenerDiagnostics.title(this, x.title.ifBlank { "(kosong)" })
        ListenerDiagnostics.body(this, x.body.ifBlank { "(kosong)" })
        ListenerDiagnostics.extraction(this, "${x.count} bagian [${x.sources.joinToString()}], channel=${sbn.notification.channelId ?: "-"}")
        ListenerDiagnostics.notification(this, x.combined.take(800).ifBlank { "(teks notifikasi kosong)" })

        val parsed = PaymentNotificationParser.parse(sbn.packageName, x.title, x.body, x.combined)
        if (parsed == null) {
            ListenerDiagnostics.parser(this, "DITOLAK: ${PaymentNotificationParser.lastReason}")
            return
        }
        ListenerDiagnostics.parser(this, "OK ${parsed.provider}: Rp${parsed.amount} — ${parsed.reason}")

        val occurred = sbn.postTime
        val rawId = "${sbn.packageName}|${sbn.id}|${sbn.tag}|${parsed.amount}|$occurred"
        val eventId = "evt_" + MessageDigest.getInstance("SHA-256").digest(rawId.toByteArray()).take(12).joinToString("") { "%02x".format(it) }
        val event = JSONObject()
            .put("event_id", eventId)
            .put("amount", parsed.amount)
            .put("source", sbn.packageName)
            .put("occurred_at_ms", occurred)
            .put("recovery", recovery)
            .put("notification_hash", MessageDigest.getInstance("SHA-256").digest(x.combined.toByteArray()).joinToString("") { "%02x".format(it) })
        EventQueue.add(this, event)
        WorkManager.getInstance(this).enqueue(OneTimeWorkRequestBuilder<UploadWorker>().build())
    }
}
