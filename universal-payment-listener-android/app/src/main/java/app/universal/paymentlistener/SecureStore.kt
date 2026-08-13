package app.universal.paymentlistener

import android.content.Context
import org.json.JSONArray

object SecureStore {
    private const val PREF = "upl_secure"
    data class Config(val baseUrl: String, val deviceId: String, val secret: String, val allowedPackages: Set<String>)

    fun save(context: Context, c: Config) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString("base_url", c.baseUrl)
            .putString("device_id", c.deviceId)
            .putString("secret", c.secret)
            .putString("allowed_packages", JSONArray(c.allowedPackages.toList()).toString())
            .apply()
    }

    fun load(context: Context): Config? {
        val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val base = p.getString("base_url", null) ?: return null
        val id = p.getString("device_id", null) ?: return null
        val secret = p.getString("secret", null) ?: return null
        val arr = try { JSONArray(p.getString("allowed_packages", "[]")) } catch (_: Exception) { JSONArray() }
        return Config(base, id, secret, (0 until arr.length()).mapNotNull { arr.optString(it) }.toSet())
    }

    fun clear(context: Context) = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().apply()
}
