package app.universal.paymentlistener

import android.content.Context

object ListenerDiagnostics {
    private const val PREF = "upl_diagnostics"

    private fun put(context: Context, key: String, value: String) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(key, value).apply()
    }

    fun notification(context: Context, value: String) = put(context, "last_notification", value)
    fun title(context: Context, value: String) = put(context, "last_title", value)
    fun body(context: Context, value: String) = put(context, "last_body", value)
    fun extraction(context: Context, value: String) = put(context, "last_extraction", value)
    fun parser(context: Context, value: String) = put(context, "last_parser", value)
    fun upload(context: Context, value: String) = put(context, "last_upload", value)

    data class Snapshot(val notification: String, val title: String, val body: String, val extraction: String, val parser: String, val upload: String)

    fun read(context: Context): Snapshot {
        val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return Snapshot(
            p.getString("last_notification", "-") ?: "-",
            p.getString("last_title", "-") ?: "-",
            p.getString("last_body", "-") ?: "-",
            p.getString("last_extraction", "-") ?: "-",
            p.getString("last_parser", "-") ?: "-",
            p.getString("last_upload", "-") ?: "-"
        )
    }
}
