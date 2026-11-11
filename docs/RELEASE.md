# Rilis Scanly 1.7.0 melalui GitHub

Target distribusi adalah GitHub; Play Store ditunda. Source berada di Handikaramadhan/Scanly, dan APK/ZIP/checksum ditempatkan di GitHub Releases. Panduan lengkap: [GITHUB_RELEASE.md](GITHUB_RELEASE.md).

## Kandidat yang disiapkan

- Scanly-1.7.0-release-test.apk: optimized, tidak debuggable, key uji lokal yang sama dengan APK percobaan sebelumnya.
- Scanly-1.7.0-debug.apk: build diagnosis; bukan APK utama distribusi.
- Scanly-1.7.0-source.zip, SHA256SUMS.txt, validation-summary.json: source publik dan bukti pemeriksaan.
- Release APK/AAB unsigned tetap tersedia untuk signing lanjutan, tetapi jangan diberikan sebagai APK install kepada pengguna.

Rilis awal sebaiknya ditandai Pre-release karena akurasi/alur native masih perlu uji HP. Tinjau RELEASE_NOTES.md dan DEVICE_TESTS.md. Tidak ada keystore atau data pengguna yang diunggah bersama source.

## Build lokal

JDK 21, Android SDK 36, Build Tools 35.0.0, Python 3. Build pertama memerlukan dependency internet.

```sh
scripts/check.sh
env JAVA_HOME=/path/ke/jdk21 python3 scripts/package_artifacts.py --test-release
python3 scripts/package_source.py
python3 scripts/write_checksums.py
cd artifacts
sha256sum -c SHA256SUMS.txt
```

--test-release hanya memakai keystore debug lokal yang telah ada dan memverifikasi sertifikatnya terhadap APK debug. Source ZIP memakai allowlist yang sama dengan publikasi GitHub. Arsip workspace/draft toko/screenshot kamera kustom lama tidak dimasukkan.

## Identitas pembaruan

Android memerlukan package dan sertifikat yang sama untuk update. Simpan key distribusi secara privat dan konsisten. Jangan menandatangani rilis pengguna dengan key debug baru dari setiap runner CI. Jangan commit key/password atau menjadikan key sebagai artifact/cache workflow yang dapat diakses kontribusi fork.

Untuk signing stabil, pemilik menyiapkan keystore di luar source dan release-signing.properties lokal yang diabaikan Git:

```properties
storeFile=/path/privat/scanly-release.jks
storePassword=ISI_SENDIRI_DI_FILE_LOKAL
keyAlias=scanly-release
keyPassword=ISI_SENDIRI_DI_FILE_LOKAL
```

Batasi akses file tersebut. Build dengan ./gradlew lintRelease assembleRelease; verifikasi apksigner. Skrip packaging kandidat unsigned sengaja tidak melabeli hasil signed penerbit sebagai unsigned. Jika identitas/key berubah, ekspor PDF/TXT penting sebelum mengubah instalasi; folder/indeks OCR tidak ikut ekspor PDF.

## Batas bukti

61 tes lokal dan lint tanpa error tersedia. Renderer/recognizer injeksi bukan bukti kamera/OCR/konversi HP. Lihat VALIDATION.md; lampirkan bukti perangkat saat tersedia. TTD adalah gambar visual pada salinan raster, penghapus latar fokus tinta/kertas dan source PDF tetap disimpan.
