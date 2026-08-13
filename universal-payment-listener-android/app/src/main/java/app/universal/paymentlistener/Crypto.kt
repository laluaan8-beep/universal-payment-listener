package app.universal.paymentlistener

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Crypto {
    fun hmacSha256Hex(secret: String, value: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}
