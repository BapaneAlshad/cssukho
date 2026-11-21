# Verifikasi Tahap 3 — OAuth Instagram SUKHO RAMEN

Tanggal: 2026-11-21

## Implementasi

- Migrasi `backend/db/002_instagram_oauth.sql` diterapkan ke proyek Supabase `iylnbmvqhldyarckqsft`:
  - `sukho_private.oauth_states` untuk state OAuth satu kali dan kedaluwarsa 10 menit.
  - `sukho_private.platform_tokens` untuk ciphertext token provider.
  - `sukho_private.provider_events` dengan kunci provider unik untuk deduplikasi webhook.
  - indeks foreign key tambahan untuk `admin_devices.activation_code_hash`, `oauth_states.platform`, dan `oauth_states.device_id`.
- Endpoint Vercel ditambahkan:
  - `GET /api/connect?platform=instagram`
  - `GET /api/instagram-callback`
  - `GET/POST /api/instagram-webhook`
- Token Instagram dienkripsi AES-256-GCM menggunakan kunci 32 byte yang hanya berada di environment Vercel.
- Android menambahkan tombol **Hubungkan Instagram**, membuka browser sistem, menampilkan error konfigurasi secara aman, dan refresh status saat kembali dari browser.
- TikTok tetap menampilkan **Menunggu akses API**; pengiriman live belum diaktifkan.

## Pemeriksaan lokal

```text
backend: npm run check       PASS
backend: npm test            PASS (8 tests)
Android: :app:compileDemoKotlin PASS
Android: :app:testDemoUnitTest    PASS
Android: :app:assembleDemo        PASS
Android: :app:lintDemo            PASS
Android: :app:connectedDebugAndroidTest PASS (10/10, emulator-5556)
APK smoke: `scripts/smoke-device.ps1 -Serial emulator-5556` PASS (native input, reply, completion, restart, reopen)
```

## Pemeriksaan cloud

- Deployment produksi Vercel: `dpl_7wPDd9vw2rcwndkhg6qUxoAiJ9zz`, region `sin1`, status `READY`.
- Tanpa Bearer token, `/api/bootstrap` dan `/api/connect` mengembalikan `401`.
- Sebelum kredensial Meta diisi, callback dan webhook mengembalikan `503 provider_not_configured`; tidak ada token yang bocor.
- Kredensial berikut belum diisi karena belum ada Meta App: `INSTAGRAM_APP_ID`, `INSTAGRAM_APP_SECRET`, `INSTAGRAM_TOKEN_ENCRYPTION_KEY`, `INSTAGRAM_WEBHOOK_VERIFY_TOKEN`.

## APK

- APK pengujian: `artifacts/CS-Sukho-Ramen-stage3.apk`
- SHA-256: `1F61BB30F2687FBFB9C68E067DFECDAEAE484B47DF2A4CB6F881ED8CFDC4ACC4`
- BuildConfig API: `https://sukho-ramen-api.vercel.app`
- ADB saat verifikasi hanya melihat `emulator-5556`; perangkat USB fisik tidak sedang terhubung, sehingga smoke test USB fisik perlu dijalankan saat perangkat disambungkan.

## Langkah pemilik Meta App

1. Buat/siapkan Meta App untuk Instagram API with Instagram Login dan gunakan akun Instagram Professional.
2. Daftarkan callback `https://sukho-ramen-api.vercel.app/api/instagram-callback`.
3. Isi environment variables Vercel melalui dashboard/konektor, tanpa menaruh secret di chat atau GitHub.
4. Deploy ulang, lalu uji **Hubungkan Instagram** dari APK live dan verifikasi webhook sebelum menyatakan integrasi end-to-end aktif.

