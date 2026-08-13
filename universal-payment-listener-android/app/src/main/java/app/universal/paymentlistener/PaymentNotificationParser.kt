package app.universal.paymentlistener

object PaymentNotificationParser {
    data class Parsed(val amount: Long, val provider: String, val reason: String)
    @Volatile var lastReason: String = "belum ada"
        private set

    private val rp = Regex("(?i)\\bRp\\s*([0-9][0-9.,]*)")
    private val qrisStaticTitle = Regex("(?i)pembayaran\\s+qris\\s+statis\\s+diterima")
    private val exactBody = Regex("(?i)\\bRp\\s*([0-9][0-9.]*)\\s+di\\s+.+")
    private val negative = listOf("pencairan", "payout", "ditarik", "tarik", "cashback", "promo", "voucher", "diskon")

    fun parse(packageName: String, title: String, body: String, combined: String): Parsed? {
        if (packageName == "com.gojek.gopaymerchant") return parseGoPay(title, body, combined)
        return parseGeneric(packageName, combined)
    }

    private fun parseGoPay(title: String, body: String, combined: String): Parsed? {
        val all = "$title | $body | $combined"
        if (negative.any { all.contains(it, ignoreCase = true) } && !qrisStaticTitle.containsMatchIn(title)) {
            lastReason = "GoPay bukan notifikasi pembayaran masuk"
            return null
        }
        if (!qrisStaticTitle.containsMatchIn(title) && !qrisStaticTitle.containsMatchIn(combined)) {
            lastReason = "judul GoPay bukan 'Pembayaran QRIS statis diterima'"
            return null
        }
        val target = sequenceOf(body, combined).mapNotNull { exactBody.find(it) }.firstOrNull()
        if (target != null) {
            val amount = parseAmount(target.groupValues[1])
            if (amount > 0) {
                lastReason = "GoPay QRIS statis exact"
                return Parsed(amount, "GoPay Merchant", lastReason)
            }
        }
        val amounts = rp.findAll(all).map { parseAmount(it.groupValues[1]) }.filter { it > 0 }.distinct().toList()
        if (amounts.size == 1) {
            lastReason = "GoPay QRIS statis fallback nominal tunggal"
            return Parsed(amounts.first(), "GoPay Merchant", lastReason)
        }
        lastReason = if (amounts.isEmpty()) "nominal Rp tidak ditemukan" else "nominal ambigu: ${amounts.joinToString()}"
        return null
    }

    private fun parseGeneric(packageName: String, combined: String): Parsed? {
        val lower = combined.lowercase()
        val incoming = listOf("diterima", "pembayaran", "masuk", "received", "berhasil").any { lower.contains(it) }
        val outgoing = listOf("kirim uang", "transfer keluar", "payout", "penarikan", "ditarik").any { lower.contains(it) }
        if (!incoming || outgoing) {
            lastReason = "generic: konteks incoming tidak cukup"
            return null
        }
        val amounts = rp.findAll(combined).map { parseAmount(it.groupValues[1]) }.filter { it > 0 }.distinct().toList()
        if (amounts.size != 1) {
            lastReason = "generic: nominal ${if (amounts.isEmpty()) "tidak ditemukan" else "ambigu"}"
            return null
        }
        lastReason = "generic incoming"
        return Parsed(amounts.first(), packageName, lastReason)
    }

    private fun parseAmount(raw: String): Long {
        val digits = raw.replace(Regex("[^0-9]"), "")
        return digits.toLongOrNull() ?: 0L
    }
}
