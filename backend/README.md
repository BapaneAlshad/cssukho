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

Token dibuat acak pada perangkat, dikirim hanya saat aktivasi melalui HTTPS, lalu dipakai sebagai Bearer token. Database hanya menyimpan hash SHA-256 token. Aktivasi dengan kode dan token sama dapat diulang jika respons jaringan hilang. Kode yang kedaluwarsa atau sudah dipakai oleh token lain ditolak.

## Pemeriksaan lokal

```powershell
npm install
npm run check
npm test
```

Setelah migrasi terpasang, **sebelum perangkat sungguhan dipasangkan**, set `TEST_DATABASE_URL` ke database uji tersebut lalu jalankan `npm run test:integration`. Uji ini memastikan kode kedaluwarsa/dipakai ulang, token dicabut, inbox/detail, status selesai, dan batas unik database; data uji dibersihkan di akhir. Uji menolak berjalan jika sudah ada perangkat aktif.

Uji database penuh memerlukan proyek Supabase yang dapat dikelola. Pada tahap ini, inbox cloud kosong dan API balasan sosial belum tersedia; data demo hanya ada di Android.

