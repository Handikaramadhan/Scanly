# Scanly 1.8.0

Pemindai dokumen Android gratis dengan kode aplikasi open source (Apache-2.0), tanpa akun, iklan, langganan atau watermark. Kamera, deteksi tepi, crop, filter dan editor kembali memakai **Google ML Kit Document Scanner**, dengan konfigurasi scanner penuh seperti versi awal. Komponen scanner Google sendiri tidak open source.

## Unduh dan pasang

Source resmi: [Handikaramadhan/Scanly](https://github.com/Handikaramadhan/Scanly). APK dibagikan melalui **[GitHub Releases](https://github.com/Handikaramadhan/Scanly/releases)**. Versi 1.8.0 membawa layout baru berdasarkan wireframe pemilik: empat alat di Beranda, Dokumen terpisah dari Folder, dan kartu folder yang dapat dibuka.

APK versi ini adalah **Scanly-1.8.0.apk**, menggunakan package dan kunci penerbit yang sama dengan 1.7.1. Instal sebagai pembaruan tanpa uninstall; tidak ada migrasi lokasi PDF atau katalog. Cocokkan checksum rilis. Android minimum 6.0; scanner memerlukan Google Play services kompatibel.

Pengguna APK uji 1.7.0/sebelumnya perlu ekspor PDF/TXT sebelum mengganti instalasi karena kunci APK uji berbeda. Uninstall menghapus data internal, folder dan indeks OCR. Jangan uninstall 1.7.1 untuk memperbarui ke 1.8.0.

Panduan signing dan publikasi: [GitHub Releases](docs/GITHUB_RELEASE.md). Artifact debug Actions bukan jalur pembaruan pengguna.

## Cara scan

1. Ketuk **Scan Dokumen**. Google Play services menyiapkan dan membuka scanner Google.
2. Foto atau impor dokumen melalui UI scanner. Tambahkan halaman dan gunakan crop/rotasi/filter yang disediakan Google.
3. Konfirmasi hasil. Scanly menyalin PDF ke penyimpanan internal dan menampilkannya di daftar.
4. Buka, bagikan, ekspor salinan, ubah nama atau hapus PDF dari daftar dokumen.

Tidak ada batas halaman buatan Scanly; kapasitas praktis bergantung perangkat/scanner. Tampilan kamera/editor dan pengambilan foto mengikuti Google, sehingga mode horizontal dan shutter manual kamera kustom versi 1.1–1.4 tidak digunakan lagi. UI Google didistribusikan melalui Play services dan dapat berubah; ini pemulihan komponen/configuration versi awal, bukan pembekuan tampilan lama.

## Gambar, PDF dan tanda tangan

- **Gambar → PDF:** ketuk **JPG ke PDF** di Beranda. Pilih beberapa gambar, atur urutannya dengan Naik/Turun, hapus yang tidak diperlukan, lalu Buat PDF. Orientasi EXIF diperbaiki; gambar transparan diberi dasar putih. Gambar galeri tidak diubah.
- **PDF → JPG/PNG:** ketuk **PDF ke JPG/PNG** di Beranda dan pilih PDF, atau buka menu PDF → **Ekspor JPG/PNG**. Pilih format dan halaman untuk satu berkas gambar, atau ekspor semua halaman sebagai gambar berurutan dalam ZIP. Pemilih berkas Android menentukan lokasi hasil.
- **TTD:** ketuk **Sematkan tanda tangan** di Beranda lalu pilih PDF, atau menu PDF → **Tambah tanda tangan**. Gambar dengan jari (Urungkan/Bersihkan tersedia), atau pilih foto/PNG tanda tangan. Foto diproses dengan penghapus latar lokal, lalu gunakan penggeser koreksi bila perlu. Paling cocok tinta gelap di kertas terang; bukan penghapus latar subjek/foto umum.
- Pilih halaman, ketuk/geser tanda tangan untuk posisi, atur ukuran lalu **Simpan PDF bertanda tangan**. Satu tanda tangan ditempel pada halaman yang dipilih dalam satu operasi. PDF sumber tetap tersimpan; hasil adalah salinan baru yang dapat diberi nama atau dipindahkan ke folder.

Tanda tangan berupa gambar visual, bukan sertifikat digital. Salinan bertanda tangan dirender ulang menjadi PDF gambar; lapisan teks, formulir, tautan dan anotasi interaktif PDF asal tidak dipertahankan di salinan. Ukuran halaman dipertahankan. OCR hasil lama tidak disalin ke PDF baru; jalankan OCR lagi bila diperlukan. Ekspor gambar/render tanda tangan memakai sisi maksimum 2200 px; gambar impor dinormalisasi sampai 2400 px untuk membatasi memori.

Semua proses alat berjalan lokal. Input/TTD sesi disimpan sementara di cache privat, dihapus setelah sesi ditutup normal; cache dapat tersisa bila proses dimatikan sistem. Gambar/TTD sesi yang masih tersedia dapat dipulihkan lewat state Android. Galeri/lokasi berbasis cloud tetap mengikuti koneksi penyedianya. Ekspor yang dibatalkan bisa meninggalkan berkas tidak lengkap di tujuan pilihan pengguna.

## Folder dan OCR

Tab **Dokumen** menampilkan semua PDF beserta pencarian/urut; tidak lagi memiliki chip/filter folder. Buka tab **Folder** untuk melihat kartu berbentuk folder, nama, dan jumlah dokumen. Ketuk **Buat folder**, lalu buka kartu untuk melihat isi. Menu tiga titik pada kartu atau halaman folder menyediakan rename/hapus. **Tanpa folder** mengumpulkan PDF yang belum dikelompokkan.

Menu **Pindahkan ke folder** pada PDF mengatur keanggotaannya. Menghapus folder mengembalikan isinya ke **Tanpa folder** tanpa menghapus PDF/OCR. Scan dari halaman isi folder otomatis diarahkan ke folder tersebut; scan dari Beranda/Dokumen tidak. Kembali dari isi folder membuka daftar folder. Pencarian di Dokumen dan isi folder disimpan terpisah.

Folder ditampilkan sebagai objek visual yang dapat dibuka. Pengelompokan masih dicatat di katalog lokal; tidak ada pemindahan atau penggantian lokasi PDF lama.

Menu **Baca teks (OCR)** memproses PDF yang sudah tersimpan, satu halaman setiap kali. Model Latin untuk dokumen Indonesia/Inggris disertakan di APK dan berjalan lokal. Hasil tampil per halaman, dapat diseleksi/disalin, dibagikan sebagai teks atau diekspor sebagai TXT. Pencarian **nama atau isi dokumen** mencakup hasil OCR yang sudah selesai dan mengikuti folder terpilih. Periksa hasil karena OCR bisa keliru.

**PDF asli tidak diubah atau diberi lapisan teks oleh Scanly.** Ekspor PDF tidak membawa folder/indeks OCR; ekspor TXT untuk menyimpan teks terpisah. OCR bukan layanan cloud. Ada progres dan pembatalan; hasil baru disimpan hanya setelah seluruh halaman selesai, sehingga gagal/terhenti di tengah tidak mengganti hasil lengkap sebelumnya. OCR dapat dijalankan ulang bila proses aplikasi dimatikan.

Untuk menjaga memori dan penyimpanan, hasil dibatasi 200 ribu karakter per halaman dan 2 juta karakter per proses OCR; tidak ada batas halaman scanner buatan aplikasi. Teks panjang di atas 200 ribu karakter diekspor sebagai TXT alih-alih dikirim sekaligus melalui clipboard/intent Android.

## Offline dan privasi

Komponen scanner mungkin perlu diunduh **ketika pertama kali scanner dibuka**, bukan selalu ketika app diinstal. Hubungkan internet untuk penyiapan awal dan gunakan Google Play services yang kompatibel. Setelah komponen siap, scan dan pengelolaan PDF lokal dapat dipakai offline. Lihat [panduan Google](https://developers.google.com/ml-kit/vision/doc-scanner/android).

Scanly tidak meminta izin CAMERA sendiri; akses kamera dikelola scanner Google. Google SDK dapat mengirim metrik/perangkat dan melakukan pembaruan saat jaringan tersedia. Kemampuan offline tidak berarti SDK tidak pernah mengakses jaringan. Lihat [kebijakan privasi](docs/PRIVACY_POLICY.md).

PDF tersimpan di penyimpanan internal aplikasi, backup otomatis dinonaktifkan. **Ekspor dokumen penting sebelum uninstall/hapus data atau pindah perangkat.** Update memakai package/signing key yang sama dan mempertahankan PDF yang sudah disimpan. Draft privat kamera kustom lama dibiarkan, tidak dihapus saat update.

## Pengelolaan dokumen

Tarik ke bawah dari bagian atas Beranda/Dokumen untuk memuat ulang. Pencarian nama, pengurutan terbaru/terlama/nama, rename tanpa overwrite, tema sistem/terang/gelap, bagikan dan ekspor tetap tersedia. Folder, OCR dan pencarian hasil teks tersedia lokal. Belum ada PDF searchable yang disematkan Scanly, biometrik atau cloud sync.

## Membangun dari source

APK distribusi 1.8.0 adalah `artifacts/Scanly-1.8.0.apk`: optimized, tidak debuggable, dan memakai kunci penerbit khusus. APK debug untuk diagnosis; APK/AAB unsigned perlu ditandatangani sebelum distribusi. Kunci privat tidak disertakan dalam source.

Build: JDK 21, Android SDK 36, Build Tools 35.0.0, Python 3, minSdk 23. Build pertama memerlukan internet untuk dependency. Siapkan local.properties untuk SDK lokal.

```sh
scripts/check.sh
python3 scripts/package_artifacts.py --sdk /path/ke/sdk
# Pemilik kunci penerbit saja; properties privat di luar repository:
python3 scripts/sign_release.py --sdk /path/ke/sdk --signing-properties /path/privat/signing.properties
python3 scripts/package_source.py
python3 scripts/write_checksums.py
```

Tes lokal memakai Robolectric dan shadow PDFBox khusus tes untuk batas renderer PDF native. Tes peluncuran scanner memakai Task/IntentSender injeksi; tes pipeline OCR menggunakan recognizer injeksi, tidak membuktikan akurasi native. APK instrumentasi menyertakan tes OCR asli dari PdfRenderer Android dan model Latin, yang perlu dijalankan pada HP. Pengunduhan komponen, kamera/editor Google dan penggunaan offline perlu uji HP sesuai [DEVICE_TESTS.md](docs/DEVICE_TESTS.md).

## Dokumentasi

- [Publikasi GitHub](docs/GITHUB_RELEASE.md)
- [Status persiapan rilis](docs/RELEASE_CHECKLIST.md)
- [Validasi](docs/VALIDATION.md)
- [Menyiapkan rilis](docs/RELEASE.md)
- [Changelog](docs/CHANGELOG.md)
- [Dependency](docs/DEPENDENCIES.json)
- [Rencana dan keputusan](docs/PROJECT_PLAN.md)
