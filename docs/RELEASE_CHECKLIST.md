# Status persiapan rilis Scanly 1.7.0

Kandidat untuk diuji, belum dipublikasikan. Package com.scanly.app, versionCode 10. Scanner Google tetap digunakan.

## Selesai di workspace

- [x] Gambar→PDF (urutan/EXIF), PDF→JPG/PNG/ZIP dan TTD visual dari gambar/coretan.
- [x] Pembersih latar foto tinta/kertas, posisi/ukuran TTD dan salinan raster tanpa menimpa sumber.

- [x] Folder lokal: buat, rename, pindah dokumen dan hapus folder tanpa menghapus PDF.
- [x] OCR Latin lokal: proses PDF per halaman, progres/pembatalan, simpan atomik dan pertahankan hasil lengkap saat gagal.
- [x] Pencarian nama/hasil OCR dalam folder, pembaca teks, salin/bagikan dan ekspor TXT.
- [x] PDF lama tetap di lokasi semula; jurnal memulihkan metadata jika rename terputus.
- [x] 61 tes lokal lulus. Tes native PDF dan OCR disertakan serta dikompilasi.
- [x] Privasi, lisensi, inventaris dependency, panduan perangkat dan listing diperbarui.
- [x] Paket optimized test/debug/unsigned AAB, sertifikat pembaruan, model OCR dan alignment binary diverifikasi.
- [x] ZIP source dan SHA256SUMS disertakan serta diperiksa integritasnya.

## Bukti perangkat yang masih diperlukan

- [ ] Konversi/ekspor via picker, tinta pada latar nyata, coretan/stylus, posisi TTD dan salinan PDF hasil di renderer HP.

- [ ] Update dari 1.5 tanpa uninstall dan pastikan PDF lama terbaca.
- [ ] Scanner Google: penyiapan awal online, lalu scan/crop/filter/tambah halaman dalam mode pesawat.
- [ ] OCR asli di HP: Indonesia/Inggris, beberapa halaman, foto buram/bayangan, hasil kosong, pembatalan dan proses dimatikan.
- [ ] Keyboard/dialog nama folder dan rename, TalkBack/font besar, copy/share/TXT via pemilih berkas.
- [ ] Kandidat optimized yang di-shrink diuji, termasuk native OCR dan HP 16 KB bila tersedia.

ADB terakhir tidak mendeteksi perangkat terhubung. Tidak ada /dev/kvm untuk emulator berakselerasi. Tes lokal menyuntikkan hasil OCR dan memakai shadow PDFBox pada batas PdfRenderer; bukan bukti akurasi model native.

## Distribusi GitHub

- [x] Target repository publik: Handikaramadhan/Scanly.
- [x] Source allowlist, README pemasangan, template issue dan workflow pemeriksaan disiapkan.
- [x] APK lokal, catatan preview dan checksum tersedia untuk Release assets.
- [ ] Unggah APK/ZIP/checksum ke GitHub Releases dan tandai Pre-release setelah review.
- [ ] Tentukan key privat untuk jalur rilis stabil; kandidat lokal saat ini memakai key uji yang sama dengan instalasi sebelumnya.

Play Store ditunda. Tidak ada kebutuhan akun Play Console atau form Data safety toko untuk mengunggah source/APK ke GitHub. Kebijakan privasi/ketentuan SDK tetap didokumentasikan. Workflow CI hanya membangun artifact pemeriksaan dan tidak mempublikasikan APK otomatis; key debug runner tidak menjadi identitas update pengguna.

Lihat GITHUB_RELEASE.md untuk langkah membuat Release dan perbedaan kandidat uji versus signing stabil.
