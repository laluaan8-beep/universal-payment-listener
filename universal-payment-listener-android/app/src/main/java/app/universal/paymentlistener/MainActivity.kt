package app.universal.paymentlistener

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var baseUrl: EditText
    private lateinit var pairing: EditText
    private lateinit var deviceName: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 88)

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(36,36,36,36) }
        val title = TextView(this).apply { text = "Universal Payment Listener"; textSize = 24f }
        root.addView(title)
        status = TextView(this).apply { textSize = 14f; setPadding(0,20,0,20) }
        root.addView(status)

        deviceName = input("Nama perangkat", "Android Listener")
        baseUrl = input("Base URL website", "https://example.com")
        pairing = input("Kode pairing", "")
        root.addView(deviceName); root.addView(baseUrl); root.addView(pairing)

        root.addView(Button(this).apply { text = "Hubungkan Website"; setOnClickListener { enroll() } })
        root.addView(Button(this).apply { text = "Aktifkan Akses Notifikasi"; setOnClickListener { startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) } })
        root.addView(Button(this).apply { text = "Paksa Hubungkan Ulang"; setOnClickListener { GuardianService.forceRebind(this@MainActivity); refresh() } })
        root.addView(Button(this).apply { text = "Refresh Diagnostik"; setOnClickListener { refresh() } })
        root.addView(Button(this).apply { text = "Putuskan Koneksi"; setOnClickListener { SecureStore.clear(this@MainActivity); refresh() } })

        val sv = ScrollView(this); sv.addView(root); setContentView(sv)
        scheduleHeartbeat(); GuardianService.start(this); refresh()
    }

    override fun onResume() { super.onResume(); GuardianService.start(this); refresh() }

    private fun input(hint: String, value: String): EditText = EditText(this).apply {
        this.hint = hint; setText(value); inputType = InputType.TYPE_CLASS_TEXT
    }

    private fun enroll() {
        val url = baseUrl.text.toString().trim().trimEnd('/')
        val code = pairing.text.toString().trim()
        val name = deviceName.text.toString().trim().ifBlank { "Android Listener" }
        Thread {
            try {
                val body = JSONObject().put("code", code).put("device_name", name).toString()
                val r = ApiClient.postJson("$url/api/listener/enroll", body)
                val j = ApiClient.jsonObject(r)
                if (r.code in 200..299 && j != null && j.optBoolean("ok")) {
                    SecureStore.save(this, SecureStore.Config(url, j.getString("device_id"), j.getString("secret"), j.optJSONArray("allowed_packages")?.let { a -> (0 until a.length()).map { a.getString(it) }.toSet() } ?: emptySet()))
                    runOnUiThread { GuardianService.start(this); refresh() }
                } else runOnUiThread { status.text = "Pairing gagal: HTTP ${r.code} ${r.body.take(300)}" }
            } catch (e: Exception) { runOnUiThread { status.text = "Pairing gagal: ${e.message}" } }
        }.start()
    }

    private fun scheduleHeartbeat() {
        val req = PeriodicWorkRequestBuilder<HeartbeatWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("upl-heartbeat", ExistingPeriodicWorkPolicy.UPDATE, req)
        WorkManager.getInstance(this).enqueue(OneTimeWorkRequestBuilder<HeartbeatWorker>().build())
    }

    private fun refresh() {
        val cfg = SecureStore.load(this)
        val d = ListenerDiagnostics.read(this)
        val access = PaymentNotificationListener.hasAccess(this)
        status.text = buildString {
            append("Pairing: ").append(if (cfg != null) "AKTIF" else "BELUM\n").append('\n')
            if (cfg != null) {
                append("Server: ").append(cfg.baseUrl).append('\n')
                append("Device: ").append(cfg.deviceId).append('\n')
                append("Package: ").append(cfg.allowedPackages.joinToString()).append('\n')
            }
            append("Akses notifikasi: ").append(if (access) "AKTIF" else "MATI").append('\n')
            append("Listener: ").append(if (ListenerRuntimeState.connected) "BOUND" else "UNBOUND").append('\n')
            append("Judul notif: ").append(d.title).append('\n')
            append("Isi notif: ").append(d.body).append('\n')
            append("Ekstraksi: ").append(d.extraction).append('\n')
            append("Notif terakhir: ").append(d.notification).append('\n')
            append("Parser terakhir: ").append(d.parser).append('\n')
            append("Upload terakhir: ").append(d.upload).append('\n')
            append("Queue event: ").append(EventQueue.all(this@MainActivity).size)
        }
    }
}
