package app.universal.paymentlistener

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object EventQueue {
    private const val PREF = "upl_event_queue"
    private const val KEY = "events"

    @Synchronized
    fun add(context: Context, event: JSONObject) {
        val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val a = try { JSONArray(p.getString(KEY, "[]")) } catch (_: Exception) { JSONArray() }
        a.put(event)
        while (a.length() > 200) {
            val next = JSONArray()
            for (i in 1 until a.length()) next.put(a.get(i))
            p.edit().putString(KEY, next.toString()).apply()
            return
        }
        p.edit().putString(KEY, a.toString()).apply()
    }

    @Synchronized
    fun all(context: Context): List<JSONObject> {
        val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val a = try { JSONArray(p.getString(KEY, "[]")) } catch (_: Exception) { JSONArray() }
        return (0 until a.length()).mapNotNull { a.optJSONObject(it) }
    }

    @Synchronized
    fun remove(context: Context, eventId: String) {
        val p = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val a = try { JSONArray(p.getString(KEY, "[]")) } catch (_: Exception) { JSONArray() }
        val out = JSONArray()
        for (i in 0 until a.length()) {
            val o = a.optJSONObject(i) ?: continue
            if (o.optString("event_id") != eventId) out.put(o)
        }
        p.edit().putString(KEY, out.toString()).apply()
    }
}
