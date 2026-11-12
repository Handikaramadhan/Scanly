# Status rilis Scanly 1.7.1

Target: rilis biasa di GitHub, Play Store ditunda. Package com.scanly.app, versionCode 11. Fitur aplikasi mengikuti 1.7.0; kamera/editor Google dipertahankan.

## Distribusi dan identitas

- [x] Source 1.7.0 dan APK uji telah dipublikasikan ke GitHub.
- [x] Workflow GitHub untuk commit ae36eac selesai sukses.
- [x] Pemilik memilih kunci penerbit khusus untuk jalur rilis selanjutnya.
- [x] Kunci RSA-4096 dibuat privat di luar source; fingerprint publik dipatok.
- [x] Jalur signing menolak kunci debug/perbedaan fingerprint dan memverifikasi hash APK unsigned.
- [x] Panduan transisi dari APK uji menjelaskan ekspor, instal ulang dan hilangnya metadata internal.
- [ ] APK 1.7.1/source/checksum/laporan diunggah ke Release v1.7.1.
- [ ] Pemilik menyimpan backup privat keystore dan password di lokasi terpisah.

- [x] Build 1.7.1, 61 tes, lint debug/release dan APK instrumentasi selesai sukses.
- [x] APK penerbit ditandatangani dan diverifikasi; salah fingerprint/hash ditolak tanpa menimpa output.

## Pemeriksaan versi ini

Hasil final build/tes dan identitas APK dicatat di artifacts/validation-summary.json dan release-validation.json. Lihat VALIDATION.md. Paket bertanda tangan dan checksum disiapkan lokal sebelum publikasi; jangan menggantinya dengan APK debug runner CI.

## Bukti perangkat yang masih diperlukan

- [ ] Instal APK penerbit 1.7.1, buka scanner/editor dan simpan beberapa halaman.
- [ ] Penyiapan scanner online lalu scan/crop/filter/tambah halaman dalam mode pesawat.
- [ ] OCR native Indonesia/Inggris, beberapa halaman, teks kosong/buram, pembatalan dan proses dimatikan.
- [ ] Konversi/ekspor picker, gambar EXIF, TTD foto/coretan dan penempatan pada PDF hasil.
- [ ] Folder/rename/pencarian, TalkBack/font besar, copy/share/TXT, penyimpanan hampir penuh.
- [ ] Pembaruan berikutnya memakai package/kunci penerbit yang sama tanpa kehilangan PDF.
- [ ] HP dengan page size 16 KB bila tersedia; pemeriksaan ELF/ZIP bukan bukti runtime.

ADB pada persiapan 1.7.1 tidak mendeteksi HP. Native PDF/OCR/konversi test APK dikompilasi, belum dieksekusi. Jangan menandai item perangkat selesai hanya karena tes lokal lulus atau label rilis diubah.
