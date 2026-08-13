# Universal Payment Listener v1.0.2 — GoPay Exact Parser

Format notifikasi GoPay Merchant yang dijadikan acuan:

```text
Pembayaran QRIS statis diterima
Rp 1.127 di JAJAN.AI DIY YOGYAKARTA, Digital & Kreatif.
```

Parser v1.0.2 melakukan validasi berlapis:

- package harus `com.gojek.gopaymerchant`;
- judul harus mengandung `Pembayaran QRIS statis diterima`;
- nominal diambil terutama dari pola `Rp <angka> di <merchant>`;
- notifikasi payout, pencairan, promo, cashback, dan info umum ditolak;
- jika ditemukan lebih dari satu nominal berbeda dan tidak ada pola QRIS yang tegas, event ditolak sebagai ambigu;
- parser generic tetap dipertahankan untuk provider lain.

Contoh:

`Rp 1.127` → `1127`

Diagnostics menampilkan judul, isi notifikasi, channel, hasil extractor, parser, dan nominal agar kegagalan berikutnya bisa dianalisis tanpa menebak.
