# Arsitektur dan lingkup Scanly

Aplikasi Android Kotlin/Jetpack Compose, minSdk 23, target/compile SDK 36, JDK 21. Kode aplikasi Apache-2.0; scanner Google dan OCR ML Kit adalah komponen pihak ketiga dengan ketentuannya sendiri.

## Fitur aktif 1.8

- Layout Beranda dengan empat alat, halaman Dokumen untuk seluruh library dan tab Folder tersendiri dengan kartu folder.
- Scanner/editor Google mode FULL, impor galeri dan hasil PDF tanpa page limit buatan aplikasi.
- PDF privat: daftar, pencarian nama/hasil OCR, folder, rename, buka, bagikan, ekspor dan hapus.
- OCR Latin bundled: PdfRenderer per halaman → recognizer lokal → transaksi teks per halaman di SQLite. PDF asli tidak ditambah lapisan teks.
- Gambar → PDF; PDF → JPG/PNG per halaman atau ZIP seluruh halaman.
- TTD visual dari coretan/foto, penghapus latar tinta/kertas, posisi/ukuran pada satu halaman dan salinan PDF raster.

## Penyimpanan dan batas

PDF berada di files/documents. catalog.db menyimpan folder/teks OCR serta jurnal rename. Cache alat menyimpan input dan TTD sesi; dibersihkan saat sesi ditutup normal, dapat tersisa setelah proses mati. Backup otomatis dinonaktifkan.

Repository memvalidasi PDF sebelum commit, memakai temporary file dan menolak overwrite nama/ekspor sumber. Hasil OCR hanya mengganti versi sebelumnya setelah seluruh halaman selesai; fingerprint sumber diperiksa. Hapus folder tidak menghapus PDF.

Scanner membutuhkan Play services kompatibel dan bisa memerlukan unduhan awal. Model OCR Latin ikut APK. Tujuan galeri/ekspor cloud mengikuti koneksi penyedianya. SDK Google dapat mengirim metrik sesuai kebijakannya, walaupun pemrosesan dokumen lokal.

Tidak ada akun, backend dokumen, iklan, langganan, watermark, cloud sync, biometrik atau tanda tangan bersertifikat. TTD adalah gambar, salinannya dirender ulang tanpa lapisan interaktif asal. Penghapus latar ditujukan untuk tanda tangan tinta di kertas, bukan segmentasi subjek foto umum.

## Pengujian dan distribusi

Tes lokal memakai Robolectric/native bitmap, PDFBox khusus tes dan recognizer/renderer injeksi. Test perangkat memakai PdfRenderer dan OCR model asli; hasil uji lokal bukan bukti akurasi/kinerja HP. Lihat VALIDATION.md dan DEVICE_TESTS.md.

Source dan APK ditujukan untuk GitHub, bukan penerbitan Play Store saat ini. Source, kandidat APK serta checksum dipisahkan; keystore, cache, SDK dan data pengguna tidak masuk Git. Build CI bukan rilis publik dan menggunakan key debug runner yang tidak stabil. APK rilis harus mempertahankan key pembaruan yang dikelola pemilik proyek.
