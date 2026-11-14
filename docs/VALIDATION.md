# Validasi Scanly 1.9.0

Tanggal: 14 November 2026. versionCode 13. Perubahan: warna fitur Beranda, Folder berupa daftar satu kolom, editor warna HEX/ikon dengan nilai bawaan, dan migrasi katalog 1→2. Lokasi PDF dan identitas signing tetap sama dengan 1.7.1/1.8.0.

## Hasil lokal

**74 tes lulus, 0 gagal/error, 0 dilewati.** Lint debug/release: **0 error, 16 warning**, berupa versi dependency/build tools dan saran KTX. Inventaris runtime tetap 138 module; EXIF langsung dipatok ke 1.4.1. Native PDF/OCR/konversi test APK dikompilasi, belum dijalankan di HP.

| Suite | Tes |
| --- | ---: |
| PdfStoreTest | 16 |
| DocumentRepositoryTest | 4 |
| DocumentCatalogTest | 12 |
| PdfOcrEngineTest | 4 |
| ScannerLauncherTest | 6 |
| ScanlyFlowTest | 13 |
| LibraryFlowTest | 6 |
| DocumentToolsTest | 8 |
| DocumentToolsModelTest | 3 |
| DocumentToolsUiTest | 2 |

Tes tambahan 1.8 memeriksa pemilihan PDF untuk dua mode alat, library kosong, isolasi filter/pencarian folder dari Dokumen, tombol Kembali, pemulihan Activity produksi, serta layar 320 dp/font 150%. Screenshot memakai view produksi dengan data PDF sintetis. Tes UI menggunakan halaman pemilih penuh; dialog rename PDF tetap mengikuti batas simulator yang sudah dicatat.

Tes pipeline alat memeriksa orientasi EXIF, gambar→PDF dengan urutan/rasio dan pixel warna, output PNG halaman terpilih, ZIP JPEG bernama urut, penutupan renderer/bitmap saat cancel, posisi/bounds TTD, hanya halaman target berubah, ukuran halaman sumber, tinta biru/latar berbayang ringan, PNG transparan, penolakan foto kosong, coretan/undo/clear/apply, koordinat preview Fit, pemulihan urutan sesi, commit idempoten dan antrean hasil galeri saat init. Routing Activity internal dan pembatalan juga diuji.

## Implementasi dan batas

- Input galeri disalin/dirotasi sesuai EXIF ke cache privat sampai 2400 px; gambar sumber tidak diubah. Signature photo dibatasi 1200 px sebelum penghapus latar.
- Gambar disusun pengguna dan dijadikan JPEG berurutan dengan dasar putih, lalu ditulis streaming ke PDF sesuai rasio gambar. Teks PDF tidak dibuat dari OCR pada proses ini.
- PDF ke gambar merender sampai sisi 2200 px. Satu halaman ditulis langsung JPG/PNG melalui picker Android; semua halaman diekspor sebagai ZIP dengan nama halaman-N.jpg/png. Render dan kompresi hanya menahan satu halaman setiap kali.
- TTD dari coretan diekspor transparan; foto TTD memakai selisih luminance lokal terhadap kertas dengan koreksi strength dan trim alpha. PNG yang sudah transparan dipertahankan. Paling cocok tinta di kertas, bukan segmentasi latar foto umum atau jaminan mengatasi semua bayangan/latar kompleks.
- TTD ditempel sebagai gambar visual pada satu halaman terpilih per operasi, dengan posisi/ukuran dinormalisasi terhadap area halaman Fit. Seluruh PDF dirender ulang menjadi salinan gambar dengan ukuran halaman asli. Formulir, tautan, lapisan teks dan anotasi interaktif tidak dipertahankan di salinan; PDF asli dan metadata OCR-nya tidak ditimpa. PDF baru tidak mewarisi OCR lama.
- Hasil PDF masuk repository melalui commit tervalidasi/idempoten berdasarkan UUID sesi. Berkas parsial tidak masuk library. Ekspor eksternal yang dibatalkan dapat meninggalkan berkas tidak lengkap di lokasi tujuan.
- Cache sesi hanya privat, dibersihkan pada penutupan normal setelah job selesai. Cache dapat tersisa bila proses dimatikan; Android juga dapat menghapusnya. Gambar/TTD belum menjadi template permanen. SavedStateHandle memulihkan sesi yang cache-nya masih ada, serta callback picker diantrikan saat init.

## Bukti dan batas pengujian

Robolectric memakai PDFBox khusus tes sebagai parser/shadow pada batas PdfRenderer. Pipeline konversi/sign diuji dengan renderer halaman yang disuntikkan dan bitmap native graphics; ini bukan pengujian renderer Android asli. Screenshot tools-signature-* memakai komponen produksi dengan coretan/PDF sintetis, bukan dokumen/TTD pribadi.

Tes perangkat menambahkan roundtrip gambar→PDF→TTD→PNG menggunakan PdfRenderer Android dan pemeriksaan pixel serta sumber utuh. Bersama roundtrip repository PDF dan OCR Latin native, total tiga tes perangkat dikompilasi. ADB terbaru melaporkan tidak ada perangkat terhubung. Kamera Google, akurasi OCR, picker/renderer perangkat dan hasil tanda tangan di HP tetap perlu diuji melalui DEVICE_TESTS.md; tidak ada klaim eksekusi perangkat atau akurasi latar universal.

Editor folder kini halaman penuh: pemilihan warna/ikon, HEX valid/invalid, simpan, reset dan pemulihan draft diuji lewat Compose. Keyboard fisik/IME dan TalkBack tetap perlu HP. Dialog rename PDF masih memiliki batas idling simulator yang sudah dicatat.

## Reproduksi dan paket

```sh
env JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew writeDependencyInventory --no-daemon --console=plain
python3 scripts/generate_notices.py
env JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew testDebugUnitTest lintDebug lintRelease assembleDebug assembleDebugAndroidTest assembleRelease bundleRelease --no-daemon --console=plain
env JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 python3 scripts/package_artifacts.py
env JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 python3 scripts/sign_release.py --signing-properties /path/privat/signing.properties
python3 scripts/package_source.py
python3 scripts/write_checksums.py
```

Identitas tetap com.scanly.app. APK distribusi Scanly-1.9.0.apk memakai kunci penerbit RSA-4096 yang berbeda dari APK uji; tidak debuggable dan fingerprint cocok dengan docs/release-certificate.sha256. APK/AAB unsigned tetap tersedia untuk build/signing terpisah. APK penerbit 1.7.1 memakai kunci yang sama sehingga pembaruan ini tidak memerlukan uninstall. APK uji 1.7.0/sebelumnya memakai kunci berbeda dan perlu ekspor sebelum penggantian instalasi. Model OCR Latin, aset lisensi/privasi, izin, ZIP dan alignment diperiksa: ELF64 minimal 16 KB, ELF32 minimal 4 KB, zipalign -P 16; ini pemeriksaan binary, bukan runtime HP 16 KB.

Tidak ada izin kamera/mikrofon/penyimpanan luas baru; alat memakai pemilih foto/berkas Android dan Activity alat tidak exported. Detail binary, hash dan sertifikat ada di artifacts/validation-summary.json dan SHA256SUMS.txt. Persiapan publikasi/identitas penerbit mengikuti RELEASE_CHECKLIST.md.

## Pemeriksaan signing 1.9.0

Skrip sign_release.py mencocokkan hash APK unsigned dengan validation-summary.json sebelum signing, kemudian memverifikasi signature, fingerprint penerbit, package/version, flag debuggable dan zipalign. Laporan release-validation.json berisi hash APK akhir dan fingerprint publik; tidak berisi password atau lokasi kunci privat. Berkas privat tidak dimasukkan ke Git/source ZIP.

Jalur signing tetap mematok sertifikat penerbit yang sama. Pemeriksaan hash/fingerprint menolak perbedaan sebelum mengganti output. Uji negatif jalur signing berasal dari persiapan 1.7.1; pada 1.9.0 pemeriksaan signature/fingerprint/versi/alignment dijalankan kembali pada APK hasil.

Build optimized APK/AAB selesai sukses; pemeriksaan debug, seluruh 74 tes, lint debug/release serta APK instrumentasi juga selesai sukses pada source final. Tidak ada tes yang dilewati. Signing dan checksum diverifikasi terpisah pada paket akhir.

## Katalog dan tampilan 1.9

Tes migrasi membuat schema SQLite versi 1 yang sama dengan aplikasi lama, lengkap folder, relasi PDF, OCR dan jurnal rename terputus. Pembukaan versi 2 memberi warna/ikon bawaan, pemulihan rename mempertahankan OCR/relasi, byte/lokasi PDF sama dan foreign_key_check kosong. Tidak ada tabel yang dihapus.

Tes tambahan mencakup persistensi warna/ikon setelah rename/reset, penolakan HEX/ikon/nama duplikat tanpa update parsial, fallback nilai tampilan tak dikenal, pilihan palet/ikon via UI, kode HEX sendiri, bawaan, draft setelah Activity dibuat ulang, dan tema gelap dengan folder putih. Label dan centang/outline menunjukkan pilihan selain warna. Notifikasi editor diposisikan di atas footer Simpan.

Kontras teks delapan pasangan warna alat (empat terang, empat gelap) dihitung dari luminance sRGB; nilai minimum 6,62:1. Ikon folder custom memakai glyph hitam/putih berdasarkan luminance. Screenshot sintetis di UI_LAYOUT.md, bukan bukti render HP.
