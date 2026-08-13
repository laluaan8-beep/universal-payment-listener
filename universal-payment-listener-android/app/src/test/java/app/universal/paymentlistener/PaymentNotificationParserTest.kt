package app.universal.paymentlistener

import org.junit.Assert.*
import org.junit.Test

class PaymentNotificationParserTest {
    @Test fun parsesRealGoPayStaticQrisNotification() {
        val p = PaymentNotificationParser.parse(
            "com.gojek.gopaymerchant",
            "Pembayaran QRIS statis diterima",
            "Rp 1.127 di JAJAN.AI DIY YOGYAKARTA, Digital & Kreatif.",
            "Pembayaran QRIS statis diterima | Rp 1.127 di JAJAN.AI DIY YOGYAKARTA, Digital & Kreatif."
        )
        assertNotNull(p)
        assertEquals(1127L, p!!.amount)
    }

    @Test fun rejectsPromoWithAmount() {
        val p = PaymentNotificationParser.parse("com.gojek.gopaymerchant", "Promo", "Cashback Rp 10.000", "Promo Cashback Rp 10.000")
        assertNull(p)
    }

    @Test fun rejectsOtherGoPayTitle() {
        val p = PaymentNotificationParser.parse("com.gojek.gopaymerchant", "Saldo bertambah", "Rp 1.127", "Saldo bertambah Rp 1.127")
        assertNull(p)
    }

    @Test fun parsesThousandSeparators() {
        val p = PaymentNotificationParser.parse("com.gojek.gopaymerchant", "Pembayaran QRIS statis diterima", "Rp 125.137 di TOKO", "Pembayaran QRIS statis diterima Rp 125.137 di TOKO")
        assertEquals(125137L, p!!.amount)
    }
}
