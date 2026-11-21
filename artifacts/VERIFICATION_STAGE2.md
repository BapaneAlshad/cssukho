# Verifikasi tahap 2 — cloud + lokal

Tanggal: **21 November 2026 (Asia/Jakarta)**. Build Android **0.2.0**.

## Hasil

| Pemeriksaan | Hasil |
|---|---|
| TypeScript backend (`npm run check`) | Lulus |
| Uji backend (`npm test`) | 4/4 lulus: kode/token, endpoint terlindungi, input aktivasi |
| Android unit test | 12/12 lulus |
| Android lint | 0 error, 23 peringatan; peringatan `commit()` disengaja untuk persistensi kredensial/cooldown |
| UI instrumentasi API 29 | 10/10 lulus, termasuk PIN, demo, Pengaturan → DM/Comment |
| UI instrumentasi API 36 | 10/10 lulus |
| Smoke APK teroptimasi API 29 dan API 36 | Lulus: sentuh, keyboard, balasan demo, selesai, buka kembali setelah proses dihentikan |
| Signature APK | Valid, skema v2, development signing |
| Smoke APK live API 36 | Lulus build dan pemasangan; URL API production tertanam pada BuildConfig |
| USB fisik | Lulus smoke test pada Infinix X6833B: input, balasan, selesai, restart, buka kembali |

APK live: `CS-Sukho-Ramen-demo.apk` (2.753.805 byte; SHA-256 `2F8C13F95E0C49C20DC0FADADC415E987D25FAE3C2BF8D14093E02F91EC8E7DB`). Build memakai `SUKHO_API_BASE_URL=https://sukho-ramen-api.vercel.app`; varian `demo` tetap tersedia dan data demo terpisah dari sesi cloud.

## Verifikasi cloud

- Supabase project `iylnbmvqhldyarckqsft` aktif dengan skema privat `sukho_private`; tabel perangkat, kode aktivasi, koneksi platform, interaksi, dan pesan sudah dibuat, RLS aktif, serta status awal Instagram `disconnected` dan TikTok `pending_access`.
- Vercel project `sukho-ramen-api` production aktif di `https://sukho-ramen-api.vercel.app`, deployment final `dpl_6Ebwfwhc4ue6rxDmBECY3sCehAQL`, region function `sin1`.
- Aktivasi kode valid berhasil; aktivasi ulang idempoten berhasil; kode kedaluwarsa menghasilkan `activation_expired`; kode yang sudah dipakai ulang menghasilkan `activation_used`; token tanpa kredensial, token acak, dan token yang dicabut menghasilkan `401 unauthorized`.
- `/api/bootstrap` mengembalikan status akun yang sebenarnya; `/api/inbox` dan detail interaksi berhasil membaca fixture sementara; PATCH selesai tersimpan dan terbaca kembali. Semua fixture uji kemudian dihapus; verifikasi pasca-pembersihan menunjukkan nol baris uji.
- Backend final dan Android build lulus setelah logging diagnostik host/error dihapus.
- `connectedDebugAndroidTest` menyelesaikan 10/10 pada emulator API 36. Pada ponsel USB, satu uji Compose terisolasi gagal karena hierarki Compose tidak tersedia saat runner berjalan; smoke test native di ponsel yang sama lulus seluruh alur APK.

## Catatan

Proyek Supabase yang terhubung saat ini berregion `ap-southeast-2` (Sydney), sedangkan Vercel Function berada di Singapura (`sin1`). Jika persyaratan region Supabase harus tepat Singapura (`ap-southeast-1`), proyek Supabase perlu dibuat ulang dan dimigrasikan dengan persetujuan pengguna.

Data Instagram/TikTok dan pengiriman balasan live belum tersedia pada tahap ini. Semua balasan yang diuji berasal dari simulator lokal dan bertanda demo.

