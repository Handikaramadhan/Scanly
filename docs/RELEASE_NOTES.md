## Scanly 1.7.0 Preview

Aplikasi Android untuk scan dan mengelola PDF, tanpa akun, iklan, langganan atau watermark.

- Scanner dan editor Google dengan deteksi tepi/crop/filter.
- Folder lokal, pencarian nama/hasil OCR, rename, bagikan dan ekspor.
- OCR Latin lokal, salin/bagikan teks dan ekspor TXT.
- Gambar ke PDF dengan pengaturan urutan; ekspor halaman PDF ke JPG/PNG atau ZIP.
- Tanda tangan visual dari coretan/foto, pembersih latar tinta/kertas dan penempatan pada halaman pilihan.

### Pemasangan

Unduh Scanly-1.7.0-release-test.apk dan cocokkan SHA-256. Ini kandidat dengan key uji lokal, bukan key produksi. Pembaruan memerlukan package/sertifikat yang sama. Ekspor dokumen penting sebelum mengganti identitas instalasi.

### Offline dan komponen

Scanner Google mungkin perlu mengunduh komponen saat pertama digunakan. Model OCR Latin ikut APK. Kode Scanly Apache-2.0; komponen Google memiliki ketentuannya sendiri dan dapat memakai jaringan untuk metrik/pembaruan sesuai kebijakannya.

### Batas pengujian

61 tes lokal lulus, lint 0 error. Tes lokal memakai renderer/recognizer injeksi; akurasi dan alur native masih perlu diuji di HP. Penghapus latar paling cocok tinta di kertas terang. Salinan PDF bertanda tangan dirender ulang menjadi gambar sehingga lapisan interaktif tidak dipertahankan; PDF asli tetap utuh. Kandidat ini belum dinyatakan stabil untuk distribusi luas.
