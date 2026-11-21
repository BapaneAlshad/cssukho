# SUKHO RAMEN · Android + backend tahap 3

Aplikasi Android native ringan untuk demo inbox CS Instagram dan TikTok, dengan fondasi backend cloud yang terpisah. Desain biru-putih, logo dan nama aplikasi **SUKHO RAMEN**, Kotlin, Jetpack Compose, Material 3. Minimum **Android 10 (API 29)**.

## Menjalankan

Buka folder ini di Android Studio, tunggu Gradle Sync, lalu jalankan konfigurasi `app` pada emulator/perangkat Android 10+. Pilih **Coba Demo** untuk data contoh. Mode cloud memerlukan backend yang sudah dideploy, kode aktivasi sekali pakai, dan PIN 6 angka yang dibuat pada perangkat.

Untuk build dari PowerShell:

```powershell
.\scripts\build.ps1
```

Script memakai JDK bawaan Android Studio jika `JAVA_HOME` belum diatur, serta SDK dari `ANDROID_HOME` atau lokasi standar Windows. Data demo tetap dapat digunakan tanpa koneksi internet. Izin jaringan dipakai hanya untuk mode cloud.

Untuk APK yang dapat mengakses backend, berikan URL deployment saat build:

```powershell
$env:SUKHO_API_BASE_URL = 'https://nama-proyek.vercel.app'
.\scripts\build.ps1
```

URL API adalah konfigurasi publik, bukan kredensial. Password database dan token perangkat tidak boleh dimasukkan ke source code atau APK.

Hasil build: `artifacts/CS-Sukho-Ramen-demo.apk`. Varian `demo` mengaktifkan R8 dan resource shrinking serta menggunakan **development signing**, untuk pengujian internal. Varian `release` tidak memiliki konfigurasi signing produksi.

## Alur yang tersedia

- Dua halaman terpisah: **DM** menampilkan DM saja, **Comment** menampilkan komentar saja. Keduanya tetap memiliki filter platform dan status, urutan terbaru, pull-to-refresh, serta tombol refresh di area kanan atas bersama tombol Pengaturan.
- Detail percakapan, konteks posting komentar, balasan, indikator Mengirim/Terkirim/Gagal, dan retry tanpa pesan ganda.
- Tandai selesai/buka kembali. Balasan, draft, status selesai, dan tema tersimpan lewat DataStore.
- Input balasan diperbarui langsung; penyimpanan draft memakai jeda 250 ms dan di-flush saat keluar dari detail/meninggalkan aplikasi.
- Tema terang (default), gelap, dan mengikuti sistem.
- Status akun simulasi, penjelasan batas API, dan skenario demo di Pengaturan.

Semua nama pelanggan dan percakapan adalah data contoh. Jam operasional/promo dalam contoh bukan informasi bisnis resmi.

## Mencoba skenario

Buka **Pengaturan → Skenario demo**. Pilih platform untuk simulasi yang hanya memengaruhi salah satu akun, kemudian pilih kondisi:

| Kondisi | Hasil |
|---|---|
| Normal | Pengiriman dikonfirmasi simulator setelah sekitar 650 ms |
| Koneksi lambat | Refresh/kirim memerlukan sekitar 3 detik |
| Offline | Data tersimpan bisa dibaca; kirim dinonaktifkan |
| Login kedaluwarsa | Platform pilihan perlu login ulang; platform lain tetap bisa digunakan |
| Batas kirim | Kirim pertama gagal; tunggu 15 detik kemudian retry |
| Gagal kirim | Kirim pertama gagal; retry pada pesan yang sama berhasil |
| Belum didukung | Balasan platform pilihan dinonaktifkan beserta alasannya |
| Gagal memuat | Refresh menampilkan error dan mempertahankan data tersimpan |
| Inbox kosong | Menampilkan empty state tanpa menghapus data |

**Simulasikan interaksi baru** menambahkan satu interaksi dari Maya Lestari. Ketuk **Buka** pada snackbar untuk menuju detail. Pengulangan event tidak menambah duplikat. **Uji event berulang** memverifikasi perilaku ini secara langsung.

Skenario kembali ke Normal ketika proses aplikasi dimulai ulang. Tema dan data percakapan tetap tersimpan. **Reset data demo** memerlukan konfirmasi dan mempertahankan pilihan tema.

Interaksi Rafi Ramadhan menunjukkan batas waktu balasan yang berakhir. Tautan asal hanya ditampilkan jika repository menyediakan URL HTTP(S) valid; fixture tidak mengarang tautan ke konten pelanggan asli.

## Mode cloud tahap 2

- Mode cloud dibuka dengan PIN lokal 6 angka. Saat pemasangan pertama, operator membuat kode aktivasi 16 karakter yang berlaku 15 menit. Satu kode hanya untuk satu aktivasi; aktivasi baru mencabut perangkat lama.
- PIN tidak dikirim ke server. Token perangkat acak disimpan terenkripsi dengan kunci turunan PIN dan Android Keystore; lima PIN salah berturut-turut memicu jeda yang bertambah. Saat PIN terlupa, hapus akses lokal dan gunakan kode aktivasi baru.
- Backend membaca inbox dan detail, serta menyimpan status selesai. Instagram ditampilkan **Belum terhubung**, TikTok **Menunggu akses API**. Tanpa izin API resmi, inbox cloud kosong dan balasan live dinonaktifkan.
- Mode demo memakai repository dan data lokal sendiri. Berpindah ke demo mengunci sesi cloud di memori; data contoh tidak diunggah ke database.

Kode backend, migrasi SQL, dan perintah kode aktivasi ada di [backend/README.md](backend/README.md).

## Mode cloud tahap 3 — fondasi Instagram resmi

- Pengaturan live memiliki tombol **Hubungkan Instagram**. Aplikasi meminta URL OAuth dari backend lalu membuka browser sistem; PIN dan token provider tidak pernah masuk ke APK.
- Backend menyimpan state OAuth satu kali selama 10 menit, menukar kode di server, mengenkripsi token dengan AES-256-GCM, dan menyediakan endpoint verifikasi webhook idempoten.
- Akun Instagram yang dihubungkan harus akun Professional (Business atau Creator). Kredensial Meta dan kunci enkripsi hanya di environment Vercel; tanpa kredensial tersebut tombol tetap menampilkan alasan konfigurasi belum siap.
- Inbox live masih read-only kosong dan balasan tetap dinonaktifkan sampai sinkronisasi event dan pengiriman resmi di increment berikutnya. TikTok tetap **Menunggu akses API**.

## Batas integrasi saat ini

**Belum ada pengiriman balasan live, sinkronisasi webhook menjadi inbox, atau push notification.** Label “Terhubung · demo” dan “Terkirim · demo” adalah konfirmasi simulator lokal, bukan konfirmasi Instagram/TikTok. Tidak ada kredensial sosial, token provider, scraping, atau login sosial dengan password di APK.

Tahap berikutnya memetakan webhook Instagram ke interaksi, menambahkan refresh token/reauth, dan mengaktifkan balasan resmi setelah uji end-to-end. TikTok tetap bergantung pada akses endpoint yang benar-benar disetujui. Model data backend sudah memisahkan platform, tipe interaksi, konteks posting, status pengiriman, pembatasan balasan, dan status selesai.

## Struktur dan toolchain

- Satu modul `app`, satu Activity, MVVM/StateFlow, Navigation Compose, manual dependency injection.
- `data`: kontrak repository, model, simulator deterministik, fixture, codec JSON dan DataStore.
- `ui`: tema, komponen ringan, Inbox, Detail, Pengaturan.
- AGP 8.13.2, Gradle 8.14.3, Kotlin/Compose Compiler 2.3.0, Compose BOM 2025.12.01.
- Compile/target SDK 36, Java bytecode 17; build di komputer ini memakai JBR 21 bawaan Android Studio.
- Font sistem, ikon vektor, tanpa gambar unduhan atau dependency animasi berat.

## Pengujian

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest :app:assembleDemo
```

Untuk menjalankan uji UI pada satu emulator yang dipilih, pastikan emulator target saja yang terhubung melalui ADB:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

Untuk smoke test APK teroptimasi dengan sentuhan dan keyboard Android asli pada emulator dalam ukuran/font default:

```powershell
.\scripts\smoke-device.ps1 -Serial emulator-5556
```

Script ini memasang APK hasil build dan mengubah **data demo** Nadia Putri untuk memeriksa teks persis, balasan, status selesai setelah proses dimulai ulang, dan buka kembali.

Pengujian instrumentasi mencakup input cepat melalui keyboard Android dengan pemeriksaan teks persis, alur balas-selesai-buka kembali, gagal/retry, konteks komentar, offline, filter, tema, round-trip DataStore asli, serta regresi perpindahan berulang dari Pengaturan ke DM/Comment. Uji regresi memastikan filter tab dan tombol Back Android bekerja. Unit test memeriksa konfirmasi pengiriman, persistensi lintas sesi, deduplikasi, batas kirim, pembatasan akun, proses terhenti, reset saat mengirim, dan penyimpanan gagal.

Hasil pengujian aktual serta ukuran APK dicatat dalam `artifacts/VERIFICATION.md`. Screenshot hasil aplikasi tersedia di `artifacts/screenshots/`.

Hasil tahap 2 dan batas verifikasi cloud dicatat dalam [artifacts/VERIFICATION_STAGE2.md](artifacts/VERIFICATION_STAGE2.md).
Hasil tahap 3 dicatat dalam [artifacts/VERIFICATION_STAGE3.md](artifacts/VERIFICATION_STAGE3.md).
