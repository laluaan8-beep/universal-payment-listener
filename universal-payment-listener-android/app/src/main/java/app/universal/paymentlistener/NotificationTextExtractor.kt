package app.universal.paymentlistener

import android.app.Notification
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

object NotificationTextExtractor {
    data class Result(val title: String, val body: String, val combined: String, val sources: Set<String>, val count: Int)

    fun extract(context: Context, n: Notification): Result {
        val pieces = linkedSetOf<String>()
        val titles = linkedSetOf<String>()
        val bodies = linkedSetOf<String>()
        val sources = linkedSetOf<String>()

        fun add(v: Any?, source: String, title: Boolean = false) {
            val s = when (v) {
                is CharSequence -> v.toString()
                else -> null
            }?.trim().orEmpty()
            if (s.isNotBlank()) {
                pieces += s; sources += source
                if (title) titles += s else bodies += s
            }
        }

        val e = n.extras
        add(e.getCharSequence(Notification.EXTRA_TITLE), "extras:title", true)
        add(e.getCharSequence(Notification.EXTRA_TITLE_BIG), "extras:titleBig", true)
        add(e.getCharSequence(Notification.EXTRA_TEXT), "extras:text")
        add(e.getCharSequence(Notification.EXTRA_BIG_TEXT), "extras:bigText")
        add(e.getCharSequence(Notification.EXTRA_SUB_TEXT), "extras:subText")
        add(e.getCharSequence(Notification.EXTRA_SUMMARY_TEXT), "extras:summary")
        add(e.getCharSequence(Notification.EXTRA_INFO_TEXT), "extras:info")
        e.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.forEach { add(it, "extras:textLines") }
        add(n.tickerText, "ticker")
        n.actions?.forEach { add(it.title, "action") }

        for (key in e.keySet()) {
            val v = try { e.get(key) } catch (_: Exception) { null }
            if (v is CharSequence) add(v, "extras:$key")
            if (v is Array<*>) v.forEach { if (it is CharSequence) add(it, "extras:$key") }
            if (v is Bundle) for (k in v.keySet()) add(v.get(k), "extras:$key/$k")
        }

        n.publicVersion?.let {
            val p = extract(context, it)
            if (p.combined.isNotBlank()) { pieces += p.combined; sources += "publicVersion" }
        }

        fun remote(rv: android.widget.RemoteViews?, label: String) {
            if (rv == null) return
            try {
                val view = rv.apply(context, null)
                collectTexts(view).forEach { add(it, "remoteviews:$label") }
            } catch (_: Exception) {}
        }
        remote(n.contentView, "content")
        remote(n.bigContentView, "big")
        remote(n.headsUpContentView, "headsUp")

        val title = titles.firstOrNull().orEmpty()
        val body = bodies.filterNot { it == title }.joinToString(" | ")
        return Result(title, body, pieces.joinToString(" | "), sources, pieces.size)
    }

    private fun collectTexts(root: View): List<String> {
        val out = mutableListOf<String>()
        fun walk(v: View) {
            if (v is TextView) v.text?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let(out::add)
            if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        walk(root)
        return out
    }
}
