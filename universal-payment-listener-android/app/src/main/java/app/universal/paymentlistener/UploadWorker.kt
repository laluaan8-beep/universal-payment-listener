package app.universal.paymentlistener

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class UploadWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val cfg = SecureStore.load(applicationContext) ?: return Result.success()
        val events = EventQueue.all(applicationContext)
        if (events.isEmpty()) return Result.success()
        var retry = false
        for (event in events) {
            val body = event.toString()
            val ts = (System.currentTimeMillis() / 1000L).toString()
            val sig = Crypto.hmacSha256Hex(cfg.secret, "$ts.$body")
            try {
                val r = ApiClient.postJson(cfg.baseUrl.trimEnd('/') + "/api/listener/event", body, mapOf(
                    "X-Listener-Device" to cfg.deviceId,
                    "X-Listener-Timestamp" to ts,
                    "X-Listener-Signature" to sig
                ))
                if (r.code in 200..299 || r.code == 409) {
                    EventQueue.remove(applicationContext, event.optString("event_id"))
                    ListenerDiagnostics.upload(applicationContext, "OK HTTP ${r.code}: ${r.body.take(250)}")
                } else {
                    ListenerDiagnostics.upload(applicationContext, "GAGAL HTTP ${r.code}: ${r.body.take(250)}")
                    retry = true
                }
            } catch (e: Exception) {
                ListenerDiagnostics.upload(applicationContext, "GAGAL: ${e.message}")
                retry = true
            }
        }
        return if (retry) Result.retry() else Result.success()
    }
}
