# Validasi Scanly 1.7.1

Tanggal: 11 November 2026. versionCode 11. Perubahan rilis: identitas signing penerbit khusus, fingerprint dipatok dan paket publikasi. Fitur scanner Google, folder, OCR, gambar/PDF dan TTD mengikuti 1.7.0. Pemeriksaan versi ini dijalankan ulang setelah perubahan versionCode/name.

## Hasil lokal

**61 tes lulus, 0 gagal/error, 0 dilewati.** Lint debug/release: **0 error, 15 warning**, berupa versi dependency/build tools dan saran KTX. Inventaris runtime tetap 138 module; EXIF langsung dipatok ke 1.4.1. Native PDF/OCR/konversi test APK dikompilasi, belum dijalankan di HP.

| Suite | Tes |
| --- | ---: |
| PdfStoreTest | 16 |
| DocumentRepositoryTest | 4 |
| DocumentCatalogTest | 8 |
| PdfOcrEngineTest | 4 |
| ScannerLauncherTest | 6 |
| ScanlyFlowTest | 7 |
| LibraryFlowTest | 3 |
| DocumentToolsTest | 8 |
| DocumentToolsModelTest | 3 |
| DocumentToolsUiTest | 2 |

Tes baru memeriksa orientasi EXIF, gambar→PDF dengan urutan/rasio dan pixel warna, output PNG halaman terpilih, ZIP JPEG bernama urut, penutupan renderer/bitmap saat cancel, posisi/bounds TTD, hanya halaman target berubah, ukuran halaman sumber, tinta biru/latar berbayang ringan, PNG transparan, penolakan foto kosong, coretan/undo/clear/apply, koordinat preview Fit, pemulihan urutan sesi, commit idempoten dan antrean hasil galeri saat init. Routing Activity internal dan pembatalan juga diuji.

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

Input dialog nama folder tetap diuji via operasi ViewModel/katalog karena kendala idling Robolectric sebelumnya; form/keyboard masuk checklist HP.

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

Identitas tetap com.scanly.app. APK distribusi Scanly-1.7.1.apk memakai kunci penerbit RSA-4096 yang berbeda dari APK uji; tidak debuggable dan fingerprint cocok dengan docs/release-certificate.sha256. APK/AAB unsigned tetap tersedia untuk build/signing terpisah. Migrasi instalasi uji memerlukan ekspor dokumen dan instal ulang; ini bukan update langsung. Model OCR Latin, aset lisensi/privasi, izin, ZIP dan alignment diperiksa: ELF64 minimal 16 KB, ELF32 minimal 4 KB, zipalign -P 16; ini pemeriksaan binary, bukan runtime HP 16 KB.

Tidak ada izin kamera/mikrofon/penyimpanan luas baru; alat memakai pemilih foto/berkas Android dan Activity alat tidak exported. Detail binary, hash dan sertifikat ada di artifacts/validation-summary.json dan SHA256SUMS.txt. Persiapan publikasi/identitas penerbit mengikuti RELEASE_CHECKLIST.md.

## Pemeriksaan signing 1.7.1

Skrip sign_release.py mencocokkan hash APK unsigned dengan validation-summary.json sebelum signing, kemudian memverifikasi signature, fingerprint penerbit, package/version, flag debuggable dan zipalign. Laporan release-validation.json berisi hash APK akhir dan fingerprint publik; tidak berisi password atau lokasi kunci privat. Berkas privat tidak dimasukkan ke Git/source ZIP.

Build 1.7.1 selesai sukses (148 task). Signing aktual lulus dengan sertifikat penerbit yang dipatok. Dua uji negatif juga lulus: fingerprint salah dan hash APK unsigned salah ditolak, tanpa menimpa APK keluaran yang sudah ada.
