# Kontribusi ke Scanly

Scanly adalah aplikasi Android native; source aplikasi dilisensikan Apache-2.0. Kamera, deteksi tepi dan editor versi 1.7 memakai Google ML Kit Document Scanner melalui Google Play services. Komponen Google tersebut tidak open source; periksa inventaris dependency serta kebijakan privasi.

## Mulai

1. Buka proyek di Android Studio; gunakan JDK 21 untuk seluruh suite pengujian lokal.
2. Siapkan SDK Platform 36 dan Build Tools 35.0.0; isi `local.properties` dengan `sdk.dir` lokal jika diperlukan.
3. Jalankan `scripts/check.sh`. Build pertama dan runtime Robolectric pertama memerlukan internet.
4. Uji perubahan UI/scan di HP. Tes Android lokal bukan pengganti kamera, izin, orientasi sensor dan renderer Android nyata.

## Batas produk 1.7

Tidak ada akun, backend dokumen, iklan, langganan, watermark, biometrik, enkripsi aplikasi, atau cloud sync. Jangan menambah dependensi atau izin tanpa kebutuhan yang jelas. Pertahankan scanner Google sesuai keputusan pengguna. Kamera/shutter/editor mengikuti Google; jangan menggantinya dengan kamera kustom tanpa perubahan keputusan pengguna. Pemindaian dapat offline setelah komponen Google tersedia; jangan menjanjikan offline sejak instalasi atau menyebut SDK Google tidak pernah mengakses jaringan.

## Perubahan data

PDF berada di `files/documents`. Jangan memindahkan atau menimpa data pengguna secara diam-diam. Pertahankan dukungan PDF versi sebelumnya, validasi nama, pembatasan path, commit berkas atomik, FileProvider read permission, dan ekspor. Bila mengubah penyimpanan, tambahkan tes kegagalan dan rencana migrasi.

## Mengirim kontribusi

Jelaskan masalah/perilaku baru, cara menguji, serta batas pengujian. Gunakan fixture buatan tanpa dokumen pribadi. Jangan masukkan keystore, password, dokumen scan, SDK, cache build, atau kredensial ke source. Repository: https://github.com/Handikaramadhan/Scanly. Gunakan issue/PR untuk usulan dan perbaikan; jangan sertakan data pribadi. Workflow Actions memvalidasi perubahan, tidak mempublikasikan APK otomatis.

Folder dan hasil OCR memakai SQLite privat; PDF tetap di files/documents. Pertahankan jurnal rename, transaksi hasil OCR per halaman, invalidasi saat berkas berubah, dan penghapusan folder tanpa menghapus PDF. OCR native dan UI scanner Google wajib diuji di HP; recognizer injeksi bukan bukti akurasi model.

Alat gambar/PDF dan TTD memakai cache privat dan hasil salinan. Pertahankan source PDF, normalisasi EXIF, render per halaman, MIME/ZIP yang sesuai, batas memori, posisi Fit yang sama pada preview/output dan cleanup setelah job. TTD bukan sertifikat digital; jangan menjanjikan penghapus latar foto umum atau preservasi lapisan interaktif pada salinan raster.
