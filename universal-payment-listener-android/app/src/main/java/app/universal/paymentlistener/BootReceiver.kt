package app.universal.paymentlistener

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        GuardianService.start(context)
        WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<HeartbeatWorker>().build())
    }
}
