# GoPay Notification Extractor v1.0.1

Universal Payment Listener v1.0.1 memperluas cara mengambil teks notifikasi Android agar kompatibel dengan notifikasi GoPay Merchant yang dapat memakai custom RemoteViews.

Extractor membaca:

- title, text, bigText, subText, summaryText, infoText;
- textLines dan nested Bundle/CharSequence di `Notification.extras`;
- tickerText dan action titles;
- publicVersion;
- `contentView`, `bigContentView`, dan `headsUpContentView` menggunakan `RemoteViews.apply()` lalu traversal view text.

Diagnostics sekarang menyimpan jumlah bagian teks, sumber ekstraksi, preview, dan channel ID. Parser pembayaran dan kontrak event/HMAC tidak diubah oleh patch extractor ini.
