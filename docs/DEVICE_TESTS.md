# Pengujian perangkat Scanly 1.7.1

Tes lokal tidak menjalankan kamera/komponen scanner Google. Gunakan HP dengan Google Play services kompatibel; catat model, Android dan versi Play services.

1. Untuk perpindahan dari APK uji, ekspor PDF/TXT dan periksa salinannya sebelum uninstall lalu instal 1.7.1. Kunci berbeda; update langsung tidak didukung. Pastikan aplikasi baru dapat scan/simpan/buka/bagikan/ekspor/rename. Uji update tanpa kehilangan data hanya antar-build yang memakai kunci penerbit yang sama.
2. Buka Scan Dokumen dengan internet saat komponen belum tersedia. Tunggu penyiapan; kamera dan editor harus berasal dari Google, tanpa kamera kustom Scanly.
3. Uji tanpa koneksi sebelum komponen siap: pesan gagal/coba lagi jelas, daftar PDF lama tetap dapat digunakan.
4. Setelah satu scan online berhasil, aktifkan mode pesawat, tutup/buka app, scan baru, crop/rotasi/filter, tambah beberapa halaman dan simpan satu PDF.
5. Periksa kualitas tepi di pencahayaan nyata, orientasi portrait/landscape, galeri dan seluruh alat editor Google. Tidak ada jaminan layout lama tetap identik jika Play services mengubah UI.
6. Batalkan scanner: tidak membuat PDF kosong. Konfirmasi hasil: PDF tersimpan sekali dan halaman sesuai; buka melalui viewer PDF native.
7. Rotasi/background app saat menyiapkan scanner, saat scanner terbuka, dan saat hasil disimpan: tidak ada spinner tersangkut atau peluncuran ulang dari callback lama.
8. Uji perangkat/Play services tidak didukung serta penyimpanan hampir penuh; berkas parsial tidak masuk daftar dan pesan jelas.
9. Tarik Beranda/Dokumen dari bagian atas untuk refresh; pencarian/urut/rename/ekspor/delete/tema tetap berjalan. Tidak ada tombol reload kanan atas.
10. Uji TalkBack, font besar, pembatasan kamera oleh sistem dan ekspor ke aplikasi/lokasi pilihan. Kamera/shutter/tambah halaman mengikuti kemampuan UI Google, bukan kontrol kamera kustom.

Paket instrumentasi memeriksa save/rename/export/delete dengan PdfRenderer Android asli. Build APK tes bukan bukti pengujian perangkat; jalankan ./gradlew connectedDebugAndroidTest pada perangkat uji.

## Folder dan OCR 1.6

1. Setelah update, PDF lama muncul di Semua/Tanpa folder tanpa perpindahan berkas. Buat folder lewat keyboard, validasi nama kosong/duplikat, rename folder dan pilih chip filter.
2. Pindahkan PDF ke folder. Mulai scan dari folder terpilih dan pastikan hasil masuk ke sana. Hapus folder: semua PDF tetap ada di Tanpa folder, beserta hasil OCR yang sudah tersimpan.
3. Jalankan Baca teks (OCR) dari menu PDF dengan dokumen Latin Indonesia/Inggris. Model ikut APK; uji juga mode pesawat dengan PDF lama tanpa membuka scanner kamera terlebih dahulu.
4. Periksa urutan beberapa halaman, progres, hasil kosong/kurang tajam dan batas OCR. Batalkan ketika halaman sedang dibaca: tunggu sampai tugas halaman selesai, tanpa mengganti hasil lengkap sebelumnya dengan sebagian hasil.
5. Tutup/buka Activity selama OCR, lalu uji penghentian proses. Rotasi mempertahankan ViewModel; proses yang dimatikan tidak menjanjikan resume otomatis. Jalankan ulang bila perlu.
6. Cari kata dari hasil OCR yang tidak ada di nama PDF. Pencarian harus mengikuti folder; query kosong kembali menampilkan seluruh folder. PDF yang belum di-OCR hanya cocok dari namanya.
7. Lihat teks per halaman, seleksi/salin, bagikan teks dan ekspor TXT UTF-8; tutup dan buka ulang aplikasi. Hasil panjang diarahkan ke TXT. Ekspor tidak boleh menimpa PDF sumber.
8. Rename PDF setelah OCR dan pemindahan folder. Folder dan teks harus tetap terkait ke nama baru; buka/export PDF byte tetap sama. Hapus PDF menghapus hasil teks dan keanggotaannya.
9. Periksa font besar/TalkBack, dialog keyboard, picker ekspor, landscape, dan tombol footer teks yang tetap dapat dijangkau. Tes lokal menguji operasi create/rename folder via ViewModel karena idling input dialog Robolectric tidak stabil; keyboard/dialog wajib dibuktikan di HP.
10. Uji APK penerbit Scanly-1.7.1.apk yang optimized, bukan hanya debug. Jalankan tes native dengan ./gradlew connectedDebugAndroidTest untuk PdfRenderer dan OCR model Latin asli. Catat model/Android/Play services, ukuran page OS bila tersedia, tanggal dan hasil.

## Alat gambar/PDF dan TTD 1.7

- Buka Gambar ke PDF, pilih JPG/PNG (termasuk foto EXIF berotasi), ubah urutan/hapus, buat PDF, lalu bandingkan urutan/rasio dan ekspor. Sumber galeri tetap utuh.
- Menu PDF → Ekspor JPG/PNG: uji satu halaman dari awal/tengah/akhir, MIME/ekstensi, serta semua halaman dalam ZIP. Buka hasil di galeri/file manager; nama halaman berurutan.
- Gambar TTD dengan jari/stylus, undo/clear, rotasi Activity dan gunakan. Uji PNG transparan, tinta biru/hitam, kertas berbayang dan foto tanpa tinta; koreksi penghapus latar jika perlu. Hasil tidak dijanjikan baik untuk latar kompleks.
- Pilih halaman dan posisi/ukuran TTD pada preview Fit, termasuk landscape dan halaman berbeda ukuran. Simpan salinan; periksa orientasi/posisi pada renderer PDF Android dan pastikan PDF asli tidak berubah. Salinan berupa PDF gambar, bukan preservasi formulir/tautan/sertifikat.
- Batalkan picker, rotasi/proses dipulihkan saat picker terbuka, hentikan konversi batch dan uji penyimpanan penuh. Tidak ada PDF parsial yang masuk library; ekspor eksternal yang terputus mungkin perlu dihapus pengguna.
- Pastikan cache sesi dibersihkan setelah keluar normal, TTD tidak menjadi template permanen, dan output hasil tidak mewarisi indeks OCR yang tidak lagi cocok. Uji ulang OCR pada PDF hasil bila dibutuhkan.
- APK instrumentasi menambah roundtrip native gambar→PDF→TTD→PNG dengan pemeriksaan pixel serta sumber utuh. Tes ini dikompilasi, harus dijalankan di HP.
