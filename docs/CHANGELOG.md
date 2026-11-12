# 1.7.1

- Kunci penerbit khusus RSA-4096 untuk jalur rilis GitHub dan pembaruan selanjutnya.
- Signing terpisah dari build unsigned, fingerprint sertifikat dipatok, password tidak masuk argumen/log.
- Paket distribusi Scanly-1.7.1.apk, checksum dan laporan signing; panduan transisi dari APK uji diperjelas.
- versionCode 11; fitur scanner/editor Google, OCR, folder, gambar/PDF dan TTD dipertahankan.

# 1.7.0

- Konversi beberapa gambar ke PDF dengan pengaturan urutan, normalisasi EXIF dan dasar putih untuk gambar transparan.
- Ekspor satu halaman PDF ke JPG/PNG atau seluruh halaman menjadi ZIP gambar berurutan.
- TTD visual dari coretan (undo/clear) atau gambar dengan pembersih latar adaptif lokal dan kontrol koreksi.
- Penempatan/ukuran TTD pada halaman pilihan, lalu simpan sebagai salinan PDF raster dengan ukuran halaman sumber. PDF asli tidak ditimpa.
- Sesi cache privat, antrean hasil picker ketika inisialisasi, commit idempoten, render per halaman dan pembatalan saat konversi.

# 1.6.0

- Folder lokal: buat/rename/hapus, pindahkan PDF, filter daftar dan target folder untuk scan baru. Hapus folder tidak menghapus PDF.
- OCR Latin bundled ML Kit untuk PDF tersimpan, satu halaman setiap kali, dengan progres, cancel dan hasil atomik per dokumen. PDF asli tidak dimodifikasi.
- Pencarian nama/hasil OCR, pembaca teks per halaman, salin/bagikan, serta ekspor TXT UTF-8.
- Katalog SQLite menyimpan folder/teks, memulihkan rename yang terputus, menolak commit OCR jika PDF berubah, dan mempertahankan hasil lengkap saat OCR ulang gagal.
- Kandidat optimized yang ditandatangani key uji tersedia selain debug/source/unsigned APK-AAB. Persiapan rilis didokumentasikan tanpa menganggap tes lokal sebagai bukti HP atau key uji sebagai key penerbit.
- Kamera/editor Google dan pengelolaan PDF versi 1.5 dipertahankan.

# 1.5.0

- Kembali ke Google ML Kit Document Scanner mode FULL dengan galeri dan hasil PDF, tanpa batas halaman buatan aplikasi, sesuai perubahan keputusan pengguna.
- Kamera, deteksi tepi, crop/filter dan UI edit mengikuti scanner Google seperti implementasi awal. Kamera/editor kustom dan dependency CameraX/Exif dihapus dari build aktif.
- Mengembalikan status penyiapan komponen dan pesan kegagalan/perangkat tidak didukung; callback Activity yang sudah berhenti dilepas.
- PDF hasil scanner disalin melalui antrean repository yang memvalidasi sebelum commit. Pencarian, rename, tema, bagikan/ekspor dan tarik untuk refresh tetap ada.
- Tidak meminta izin kamera sendiri. Komponen awal mungkin diunduh via Play services; offline setelah komponen siap.
- Package dan signing key pembaruan dipertahankan; PDF lama dan draft privat kamera sebelumnya tidak dihapus.

# 1.4.0

- Kamera CameraX dipertahankan sesuai pilihan pengguna; tidak kembali ke scanner Google.
- Deteksi kontur dilengkapi kandidat garis untuk tepi yang terputus bayangan dan latar berkontras rendah. Getaran sudut kecil pada preview diredam; pergerakan besar langsung diikuti dan garis hilang jika dokumen tidak ditemukan.
- Editor gelap dengan preview dominan, pinch zoom/reset dua ketuk, perbandingan foto utuh, thumbnail halaman dan toolbar ringkas; layout landscape berdampingan.
- Crop layar penuh: area luar digelapkan, garis bantu/kaca pembesar saat drag, deteksi ulang, seluruh foto, dan penggeser Presisi opsional.
- Filter bergambar dengan indikator pilihan; hitam-putih memakai ambang lokal agar bayangan kertas lebih terang.
- Urungkan hingga 20 perubahan per halaman selama ViewModel sesi aktif; riwayat undo tidak disimpan setelah proses dimatikan.

# 1.3.0

- Mengembalikan deteksi tepi dokumen di preview sebelum foto, dengan garis hijau.
- Auto-crop lokal setelah capture/impor, tetap dapat dikoreksi dengan empat sudut. Foto utuh sesi dipertahankan.
- Shutter tetap manual; mode kamera, batch berkelanjutan dan tarik-untuk-refresh dipertahankan.
- Saat tepi tidak jelas, gunakan seluruh foto daripada memotong paksa.

# Perubahan Scanly

## 1.2.0

- Kamera langsung dibuka dari Scan Dokumen; dialog mode awal di Beranda dihapus.
- Layout kamera HP: preview besar dengan chrome hitam, shutter bulat di tengah bawah, galeri di kiri, serta thumbnail/jumlah halaman dan Selesai di kanan.
- Mode Satu halaman/Banyak halaman berjajar di atas shutter, dapat diketuk dan digeser horizontal. Pergantian mode disimpan ke metadata sesi.
- Mode batch dengan lebih dari satu foto tidak dapat dialihkan ke single secara diam-diam; foto tetap dipertahankan.
- Flash manual on/off melalui ikon kanan atas jika kamera mendukung; kontras status bar mengikuti layar kamera yang gelap.
- Pipeline manual, editor, PDF lama dan tarik-untuk-refresh tetap dipertahankan.
- Pratinjau UI kamera dibuat dari komponen produksi dengan gambar contoh khusus tes; pengambilan kamera/flash aktual tetap perlu uji HP.

## 1.1.0

- Kamera manual CameraX menggantikan UI ML Kit: pengambilan foto hanya melalui tombol Ambil foto.
- Pilihan Satu halaman atau Banyak halaman → satu PDF sebelum membuka kamera.
- Batch tetap di kamera setelah tiap foto; Selesai membuka pemeriksaan semua halaman.
- Editor manual crop empat sudut (drag atau slider), rotasi, filter warna/abu-abu/hitam-putih, urutan dan hapus halaman.
- Impor galeri Android, normalisasi EXIF, metadata sesi untuk recreation, dan commit PDF idempoten.
- PDF JPEG ditulis berurutan ke disk agar tidak menahan bitmap seluruh halaman di memori.
- Tombol reload kanan atas diganti tarik ke bawah di Beranda/Dokumen, dengan aksi aksesibilitas alternatif.
- ML Kit/Play services dan kebutuhan unduhan komponen awal dihapus. Izin kamera sekarang diminta Scanly; PDF lama dan identitas aplikasi tetap sama.
- Pembersih noda/bayangan AI ML Kit tidak lagi menjadi fitur; editor menyediakan crop/rotasi/filter manual.
- Versi 1.1 perlu uji kamera perangkat; laporan versi lama tidak membuktikan perilaku CameraX baru.

## 1.0.0

- Lingkup MVP dituntaskan; OCR, folder, biometrik, enkripsi tambahan dan cloud sync tetap di luar versi 1.0.
- Antrean operasi mencegah hasil scan/ekspor dilewatkan saat daftar dimuat. Ekspor dipulihkan berdasarkan ID berkas, bukan bergantung pada daftar di memori.
- Pemisahan status scanner dan operasi; kegagalan inisialisasi scanner serta pemilih lokasi ditangani.
- PDF divalidasi dengan renderer Android sebelum commit; informasi halaman di-cache. Penyalinan berkas menggunakan temporary file unik dan sinkronisasi output.
- Pilihan tema sistem/terang/gelap tersimpan; pengurutan terbaru/terlama/nama; tombol muat ulang, navigasi kembali, dan pesan panjang yang dapat ditutup.
- Ikon adaptif dengan dukungan themed icon; versi aplikasi diambil dari BuildConfig. Kontras ikon status/navigation bar mengikuti pilihan tema manual.
- Lisensi 130 dependency runtime (termasuk transitif) dihimpun dari metadata POM; parser/runtime tes tidak masuk APK.
- Tes kegagalan tambahan, tes Compose/Robolectric, dan tes perangkat native terpisah.
- Konfigurasi signing penerbit opsional; build release tanpa private properties menghasilkan unsigned APK/AAB secara eksplisit.
- Dokumentasi rilis, privasi, matriks uji perangkat, draft listing, serta skrip verifikasi dan packaging source/artefak.

## 0.2.0

- Pencarian nama dokumen di tab Dokumen, tanpa membedakan huruf besar/kecil; tombol hapus pencarian dan pesan hasil kosong.
- Menu Ubah nama dengan validasi langsung, ekstensi PDF otomatis, dan isi dialog yang bertahan saat rotasi.
- Nama kosong, tersembunyi, karakter path/kontrol, nama terlalu panjang, dan benturan nama ditolak. PDF lain tidak ditimpa.
- Nama baru tetap tersimpan setelah aplikasi dibuka ulang. Isi PDF dan waktu berkas dipertahankan saat rename pada filesystem yang diuji.
- Version code naik ke 2; application ID tetap com.scanly.app. Tidak ada migrasi atau pemindahan folder dokumen.

## 0.1.0

MVP scanner, PDF lokal, buka, bagikan, ekspor, hapus, tema sistem, dan informasi privasi/lisensi. Pengguna melaporkan versi ini berjalan tanpa bug dalam percobaannya; rincian perangkat dan cakupan pengujian belum dicatat.
