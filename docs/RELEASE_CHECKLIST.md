# Status rilis Scanly 1.8.0

Package com.scanly.app, versionCode 12. Layout mengikuti wireframe pemilik: Beranda empat alat dan dokumen baru, tab Dokumen tanpa chip folder, tab Folder dengan kartu folder yang dapat dibuka. Kamera/editor Google dipertahankan.

## Implementasi

- [x] Beranda: Scan Dokumen, PDF ke JPG/PNG, JPG ke PDF dan Sematkan tanda tangan.
- [x] Pemilih PDF dengan pencarian dan penanganan library kosong; tidak membuka alat PDF tanpa input.
- [x] Folder terpisah; kartu nama/jumlah, menu rename/hapus dan Tanpa folder.
- [x] Scan ke folder, pencarian terpisah, Kembali sesuai hierarki dan pemulihan Activity.
- [x] Dokumen lama/metadata tetap di lokasi yang sama, tanpa migrasi katalog.
- [x] Package dan fingerprint kunci penerbit 1.7.1 dipertahankan untuk update tanpa uninstall.

- [x] 66 tes lokal lulus tanpa gagal/error/skip; lint debug/release tanpa error.
- [x] Build APK debug/optimized, AAB dan APK instrumentasi selesai.

- [x] APK akhir bertanda tangan penerbit; sertifikat dibandingkan langsung dengan APK 1.7.1 dan cocok.

## Paket dan bukti

Laporan tes/lint/binary berada di artifacts/validation-summary.json. Identitas signing dan hash APK akhir di release-validation.json. Lihat VALIDATION.md untuk bukti dan batas pengujian.

- [ ] Unggah APK/source/checksum/laporan 1.8.0 ke GitHub Releases setelah pengujian pengguna.
- [ ] Backup privat keystore dan password oleh pemilik; jangan unggah ke repository/artifact.

## Pengujian HP

- [ ] Update APK penerbit 1.7.1 ke 1.8.0 tanpa uninstall, periksa PDF/folder/OCR lama.
- [ ] Kamera/editor Google, penyiapan komponen, scan beberapa halaman dan mode pesawat.
- [ ] OCR Latin native, konversi PDF/gambar, TTD, picker ekspor dan ruang penyimpanan hampir penuh.
- [ ] Pemilih PDF dengan keyboard, TalkBack, font besar dan rotasi perangkat.

Tes UI lokal, screenshot render dan keberhasilan signing bukan bukti eksekusi native/perangkat. Checklist lengkap di DEVICE_TESTS.md. Play Store tetap ditunda; target distribusi GitHub.
