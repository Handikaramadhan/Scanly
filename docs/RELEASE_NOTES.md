# Scanly 1.8.0

Aplikasi Android untuk scan dan mengelola dokumen, tanpa akun, iklan, langganan atau watermark. Distribusi melalui GitHub; Play Store menyusul.

## Fitur

- Scanner dan editor Google dengan deteksi tepi, crop dan filter.
- PDF beberapa halaman, folder, pencarian, rename, bagikan dan ekspor.
- OCR Latin lokal untuk salin/bagikan teks dan ekspor TXT.
- Gambar ke PDF dengan pengaturan urutan; PDF ke JPG/PNG atau ZIP.
- Tanda tangan dari coretan/gambar, penghapus latar tinta/kertas dan penempatan pada halaman pilihan.

## Perubahan 1.8.0

- Beranda mengikuti desain baru dengan empat tombol: Scan Dokumen, PDF ke JPG/PNG, JPG ke PDF, dan Sematkan tanda tangan.
- Tiga dokumen terbaru ditampilkan di bawah alat; ketuk dokumen untuk membuka PDF.
- Folder memiliki tab tersendiri, dengan kartu berbentuk folder dan jumlah dokumen. Buka kartu untuk melihat isi atau scan langsung ke folder tersebut.
- Dokumen menampilkan seluruh PDF tanpa deretan chip folder di atasnya. Pencarian/urut, OCR dan menu pengelolaan tetap tersedia.
- Kunci penerbit sama dengan 1.7.1: pengguna versi tersebut bisa memasang update tanpa uninstall. PDF, folder dan OCR lama tetap digunakan.

## Unduh dan pasang

Unduh **Scanly-1.8.0.apk** dari Assets dan cocokkan checksum. Memerlukan Android 6.0+; scanner memerlukan Google Play services kompatibel dan RAM minimum 1,7 GB. Izinkan pemasangan dari browser/file manager jika Android meminta.

**Pengguna APK uji 1.7.0 atau sebelumnya:** ekspor PDF penting dan TXT hasil OCR, pastikan salinannya dapat dibuka, baru uninstall APK lama lalu instal versi ini. Kunci baru berbeda sehingga tidak bisa update langsung. Uninstall menghapus seluruh data internal, folder dan OCR. Ekspor PDF tidak membawa metadata folder/OCR; belum ada pemulihan library atau impor PDF di aplikasi. Salinan PDF bisa dibuka lewat viewer/file manager. Update berikutnya pada jalur kunci penerbit yang sama dapat dipasang sebagai pembaruan.

## Offline dan privasi

Komponen scanner Google mungkin diunduh saat pertama digunakan; setelah siap, pemindaian/pengolahan lokal dapat berjalan offline. Model OCR Latin disertakan dalam APK. PDF disimpan di perangkat; ekspor sebelum uninstall/hapus data. Kode Scanly Apache-2.0; komponen Google memiliki lisensi dan kebijakan masing-masing.

## Catatan penggunaan dan pengujian

Penghapus latar paling cocok tinta gelap pada kertas terang. Tanda tangan berupa gambar visual; PDF hasil tanda tangan dirender ulang menjadi gambar dan PDF asli tetap tersimpan. Bukan tanda tangan digital bersertifikat.

Tes/lint/build dan signing diperiksa; pengujian kamera Google, akurasi OCR serta alur perangkat untuk paket ini belum dijalankan oleh otomasi karena tidak ada HP terhubung. Laporkan masalah melalui Issues tanpa melampirkan dokumen atau tanda tangan pribadi.
