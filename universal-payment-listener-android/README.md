# Universal Payment Listener Android

Aplikasi Android untuk membaca notifikasi pembayaran merchant, mem-parsing nominal, lalu mengirim event terautentikasi HMAC ke website yang sudah dipairing.

Versi saat ini: **1.0.2**.

Fitur utama:

- pairing melalui Base URL + enrollment code;
- NotificationListenerService;
- allowlist package dari server;
- parser GoPay Merchant exact untuk `Pembayaran QRIS statis diterima`;
- parser generic fallback untuk provider lain;
- extractor notifikasi yang membaca extras + RemoteViews;
- HMAC SHA-256;
- queue + retry WorkManager;
- heartbeat;
- Guardian foreground service + requestRebind;
- recovery scan notifikasi aktif;
- diagnostics.

Build:

```bash
gradle testDebugUnitTest assembleDebug
```

APK debug akan berada di:

`app/build/outputs/apk/debug/app-debug.apk`
