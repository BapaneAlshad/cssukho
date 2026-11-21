# Verifikasi frontend CS Sukho Ramen

Tanggal: **21 November 2026 (Asia/Jakarta)**. Build **0.1.0-demo**.

## APK hasil akhir

- File: `CS-Sukho-Ramen-demo.apk`
- Ukuran revisi UI: **2.704.625 byte = 2,70 MB / 2,58 MiB**, di bawah anggaran 15 MB.
- Package: `com.sukhoramen.cs`; minimum API 29, target/compile API 36.
- ABI: arm64-v8a, armeabi-v7a, x86, x86_64.
- R8 dan resource shrinking aktif. Signature diverifikasi memakai `apksigner`.
- Signing: **Android Debug/development**, untuk pengujian internal. Signing produksi belum dikonfigurasi.
- SHA-256 perbaikan navigasi: `b4e3d577b3b8805a8591b528c66561abfcc1486d4e017991152dbb7250c847c0`.

## Revisi antarmuka

- Kartu sapaan dan judul inbox besar dihapus. Tombol refresh dan Pengaturan kini berada di bawah badge Mode Demo.
- Navigasi bawah dipisah menjadi DM dan Comment; masing-masing halaman hanya memuat tipe interaksi tersebut, dengan filter platform dan status tetap tersedia.
- Logo header, konteks komentar, dan ikon aplikasi memakai logo yang dikirim pengguna. Nama aplikasi menjadi **SUKHO RAMEN**.
- Build `:app:assembleDemo` berhasil. APK terpasang dan terbuka di Infinix X6833B (Android 14) melalui USB.
- Screenshot layar fisik: `screenshots/physical/dm-final.png`.
- Perbaikan navigasi dibangun, diuji, dan APK demo dipasang ulang melalui USB.

## Hasil pemeriksaan

| Pemeriksaan | Hasil |
|---|---|
| Unit test repository | **12/12 lulus** |
| Android Lint | **0 error**, 14 peringatan informatif |
| Build debug, Android test APK, dan demo teroptimasi | **Lulus** |
| UI instrumentasi Android 10 / API 29 | **8/8 lulus** |
| UI instrumentasi Android 16 / API 36 | **8/8 lulus** |
| Smoke test APK teroptimasi pada build sebelumnya | **Lulus** pada API 29 dan API 36 |
| Pemasangan dan pembukaan APK revisi melalui USB | **Lulus**, Infinix X6833B / Android 14 |
| Signature APK | **Valid** |
| Permission jaringan | **Tidak ada** |

Peringatan lint menyangkut versi dependency/toolchain yang lebih baru serta direktori resource versi lama yang kosong. Versi proyek dikunci sesuai toolchain SDK 36; tidak ada error lint yang diabaikan melalui baseline. Orientasi portrait sengaja mengikuti PRD.

## Cakupan yang diuji

- Konfirmasi pengiriman: tetap Mengirim sampai simulator mengonfirmasi; kegagalan bisa diulang memakai identitas pesan yang sama.
- Deduplikasi event dan penekanan kirim cepat; rate limit 15 detik; batas waktu balasan; isolasi kegagalan platform; offline dengan data tersimpan.
- Persistensi status selesai, balasan, draft, dan tema; pemulihan pengiriman yang terhenti; reset ketika pengiriman masih berjalan; penyimpanan gagal dan retry.
- Keyboard Android asli: teks **“Halo Kak reservasi bisa”** dibandingkan secara persis sebelum dan setelah dikirim. Input UI langsung, penulisan draft dijeda 250 ms.
- Alur Pengaturan → DM/Comment (tab sama maupun berbeda), pemulihan filter, dan Back sistem; pengulangan transisi.
- Alur DM/Comment → Detail → Balas → Selesai → tutup proses → buka aplikasi → Selesai → Buka kembali.
- Konteks posting komentar, filter, aksesibilitas kontrol tema, serta codec JSON/DataStore asli termasuk emoji.
- Navigasi dua tab tetap terbaca pada font besar; filter masing-masing tab dipulihkan setelah Pengaturan.

## Bukti

- `ui-tests-api29.txt` dan `ui-tests-api36.txt`: hasil instrumentasi final.
- `ui-tests-settings-fix-api29.txt` dan `ui-tests-settings-fix-api36.txt`: regresi navigasi terbaru, 8/8 lulus pada kedua versi Android.
- `apk-smoke-api29.txt` dan `apk-smoke-api36.txt`: pemasangan, input sentuh/keyboard, dan persistensi setelah proses dihentikan.
- `screenshots/android10/` dan `screenshots/android16/`: inbox, detail DM/komentar, gagal kirim, offline, pengaturan, tema gelap, serta font besar.
- `preview.png`: pratinjau dua layar dari hasil aplikasi.
- Laporan unit test: `../app/build/reports/tests/testDebugUnitTest/index.html`.
- Laporan lint: `../app/build/reports/lint-results-debug.html`.

Waktu cold process yang tercatat pada smoke test build sebelumnya: **867 ms (API 29)** dan **1.071 ms (API 36)**. Ini masing-masing satu observasi emulator pada host pengembangan, bukan benchmark perangkat fisik.

## Batas hasil

Pengujian instrumentasi terbaru dilakukan pada emulator x86_64 Android 10 dan Android 16. APK dipasang dan dibuka pada Infinix X6833B Android 14 melalui USB. Seluruh interaksi dan konfirmasi kirim berasal dari **simulator lokal**. Login admin, OAuth, API Instagram/TikTok, backend, webhook, dan push asli belum diimplementasikan pada tahap frontend.

