package app.universal.paymentlistener

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONObject

class HeartbeatWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val cfg = SecureStore.load(applicationContext) ?: return Result.success()
        GuardianService.start(applicationContext)
        val body = JSONObject()
            .put("device_id", cfg.deviceId)
            .put("ready", ListenerRuntimeState.connected)
            .put("notification_access", PaymentNotificationListener.hasAccess(applicationContext))
            .put("app_version", "1.0.2")
            .toString()
        val ts = (System.currentTimeMillis() / 1000L).toString()
        val sig = Crypto.hmacSha256Hex(cfg.secret, "$ts.$body")
        return try {
            val r = ApiClient.postJson(cfg.baseUrl.trimEnd('/') + "/api/listener/heartbeat", body, mapOf(
                "X-Listener-Device" to cfg.deviceId,
                "X-Listener-Timestamp" to ts,
                "X-Listener-Signature" to sig
            ))
            if (r.code in 200..299) Result.success() else Result.retry()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
