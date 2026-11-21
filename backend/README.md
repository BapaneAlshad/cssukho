# Backend tahap 2 — SUKHO RAMEN

Vercel Functions TypeScript tanpa framework. Database menggunakan proyek Supabase khusus, skema `sukho_private`, dan koneksi **Transaction pooler**. Semua tabel aplikasi berada di skema privat; aplikasi Android hanya mengakses empat endpoint Vercel.

## Persiapan cloud

1. Hubungkan organisasi/proyek Supabase baru dan akun Vercel baru yang benar. Terapkan `db/001_initial.sql` sebagai migrasi pada proyek Supabase baru. Jangan terapkan pada proyek lain.
2. Salin URI **Transaction pooler** dari panel *Connect* Supabase ke variabel `DATABASE_URL` di Vercel. Driver menggunakan `max: 1`, `prepare: false`, dan TLS. Simpan URI sebagai secret, bukan di APK atau Git.
3. Deploy folder `backend/` sebagai root proyek Vercel, dengan region `sin1`. Periksa `GET /api/bootstrap` tanpa token: respons harus `401`.
4. Pada komputer operator yang memiliki akses database, set `DATABASE_URL` lalu jalankan `npm run issue-code`. Kode 16 karakter ditampilkan satu kali dan berlaku 15 menit. Jangan simpan kode di log yang dapat diakses orang lain.
5. Build Android dengan `SUKHO_API_BASE_URL` berisi URL deployment, lalu masukkan kode dan buat PIN pada perangkat.

## Endpoint

| Endpoint | Akses | Kegunaan |
|---|---|---|
| `POST /api/activate` | Kode sekali pakai | Daftarkan perangkat, cabut perangkat lama |
| `GET /api/bootstrap` | Bearer token perangkat | Status Instagram/TikTok |
| `GET /api/inbox?cursor=...` | Bearer token perangkat | Daftar interaksi, 50 per halaman |
| `GET /api/interaction?id=...` | Bearer token perangkat | Detail dan pesan |
| `PATCH /api/interaction?id=...` | Bearer token perangkat | `{ "completed": true/false }` |
| `GET /api/connect?platform=instagram` | Bearer token perangkat | Buat URL OAuth Instagram sekali pakai |
| `GET /api/instagram-callback` | Callback provider | Tukar kode OAuth dan simpan token terenkripsi |
| `GET/POST /api/instagram-webhook` | Provider signature | Verifikasi dan simpan event webhook secara idempoten |

Token dibuat acak pada perangkat, dikirim hanya saat aktivasi melalui HTTPS, lalu dipakai sebagai Bearer token. Database hanya menyimpan hash SHA-256 token. Aktivasi dengan kode dan token sama dapat diulang jika respons jaringan hilang. Kode yang kedaluwarsa atau sudah dipakai oleh token lain ditolak.

## Pemeriksaan lokal

```powershell
npm install
npm run check
npm test
```

Setelah migrasi terpasang, **sebelum perangkat sungguhan dipasangkan**, set `TEST_DATABASE_URL` ke database uji tersebut lalu jalankan `npm run test:integration`. Uji ini memastikan kode kedaluwarsa/dipakai ulang, token dicabut, inbox/detail, status selesai, dan batas unik database; data uji dibersihkan di akhir. Uji menolak berjalan jika sudah ada perangkat aktif.

Uji database penuh memerlukan proyek Supabase yang dapat dikelola. Pada tahap ini, inbox cloud kosong dan API balasan sosial belum tersedia; data demo hanya ada di Android.

## Tahap 3 — koneksi Instagram resmi

Tahap ini memakai Instagram API with Instagram Login. Akun yang dihubungkan harus akun Instagram Professional (Business atau Creator). Buat Meta App untuk pengujian, aktifkan produk Instagram/API yang sesuai, lalu daftarkan URL callback persis:

```text
https://sukho-ramen-api.vercel.app/api/instagram-callback
```

Set `INSTAGRAM_APP_ID`, `INSTAGRAM_APP_SECRET`, `INSTAGRAM_TOKEN_ENCRYPTION_KEY` (32 byte, base64url), dan `INSTAGRAM_WEBHOOK_VERIFY_TOKEN` langsung sebagai environment variables Vercel. Jangan menaruh nilai tersebut di chat, GitHub, atau APK. Setelah itu deploy ulang dan tekan **Hubungkan Instagram** dari Pengaturan pada aplikasi live.

Callback hanya mengonsumsi state OAuth satu kali dan state kedaluwarsa setelah 10 menit. Token provider disimpan sebagai ciphertext AES-256-GCM di skema privat. Webhook memvalidasi `X-Hub-Signature-256` dan mengabaikan event duplikat; pemetaan event menjadi inbox dan pengiriman balasan dijadwalkan untuk increment berikutnya. TikTok tetap menunggu akses resmi.
