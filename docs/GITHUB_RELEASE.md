# Rilis GitHub Scanly 1.8.0

Target distribusi saat ini GitHub; Play Store dikerjakan nanti. Rilis 1.7.1 sudah memakai kunci penerbit khusus; 1.8.0 melanjutkan identitas pembaruan yang sama. Rilis berikutnya menggunakan tag **v1.8.0**, judul **Scanly 1.8.0**, dan status rilis biasa sesuai keputusan pemilik.

## Paket distribusi

- `artifacts/Scanly-1.8.0.apk`: APK optimized, tidak debuggable, kunci penerbit khusus.
- `artifacts/Scanly-1.8.0-source.zip`: source publik tanpa SDK/cache/key.
- `artifacts/SHA256SUMS.txt`: checksum APK dan berkas pendamping.
- `artifacts/release-validation.json`: identitas versi, checksum, sertifikat penerbit dan batas pengujian.
- `artifacts/validation-summary.json`: hasil tes/lint/pemeriksaan binary.

Buka https://github.com/Handikaramadhan/Scanly/releases/new, pilih main dan tag v1.8.0. Salin docs/RELEASE_NOTES.md, lampirkan berkas di atas, lalu Publish release. Jangan centang Pre-release untuk jalur rilis biasa ini. Jangan mengganti APK pada tag 1.7.0-rc1: identitas kunci berubah dan harus dijelaskan sebagai rilis baru.

Source sudah dapat di-push melalui SSH. Pembuatan Release dan pengunggahan asset memerlukan login web atau token API yang memiliki izin menulis release; SSH Git saja tidak menyediakan operasi tersebut. Tidak ada credential API penerbit yang ditanam dalam workflow.

## Kunci pembaruan

Kunci penerbit RSA-4096 dibuat khusus Scanly. Fingerprint publik dipatok di [release-certificate.sha256](release-certificate.sha256); skrip signing menolak kunci debug atau kunci penerbit yang tidak cocok. Kunci dan password privat berada di luar repository, dengan direktori 0700 dan file 0600. Pemilik perlu menyimpan backup privat keystore dan password secara terpisah; kehilangan kunci menghalangi update dengan identitas yang sama. Jangan unggah key/password ke GitHub, issue, artifact atau log.

Sebelum pindah dari APK uji, ekspor PDF/TXT dan periksa salinannya, baru uninstall lalu instal APK rilis. Uninstall menghapus data internal. Metadata folder/OCR tidak ikut ekspor PDF; belum ada restore library/impor PDF dalam aplikasi. Jangan menjanjikan update langsung dari APK uji.

Saat nanti memakai Play App Signing, gunakan identitas app signing yang sama bila ingin update lintas GitHub/Play Store tetap kompatibel. Lihat https://developer.android.com/studio/publish/app-signing.

## Build dan signing

JDK 21, SDK 36, Build Tools 35.0.0, Python 3. Build pertama memerlukan internet.

```sh
scripts/check.sh
python3 scripts/package_artifacts.py --sdk /path/ke/sdk
python3 scripts/sign_release.py --sdk /path/ke/sdk --signing-properties /path/privat/signing.properties
python3 scripts/package_source.py
python3 scripts/write_checksums.py
```

Properties privat di luar repository: storeFile (path absolut keystore), storePassword, keyAlias, keyPassword. Keystore dan properties harus mode 0600. Jangan memakai release-signing.properties di root proyek untuk jalur ini: skrip signing bekerja dari APK unsigned yang hash-nya sudah diverifikasi. Password dilewatkan lewat environment subprocess, bukan argumen command atau laporan.

CI menjalankan tes/lint/build, menghasilkan artifact pemeriksaan dan tidak mempublikasikan APK pengguna. Kunci debug runner berbeda dari kunci penerbit.

## Bukti kualitas

Lihat VALIDATION.md dan DEVICE_TESTS.md. Tes lokal dan pemeriksaan signing tidak menggantikan uji kamera/OCR/renderer pada HP. Pemilihan label rilis biasa tidak mengubah batas bukti pengujian.
