# Scanly 1.7.1

Rilis GitHub dengan kunci penerbit khusus; Play Store ditunda. Ikuti [GITHUB_RELEASE.md](GITHUB_RELEASE.md) untuk build, signing, aset publikasi dan identitas update.

APK utama: **Scanly-1.7.1.apk**. Jangan distribusikan APK unsigned atau artifact debug CI sebagai pembaruan jalur rilis ini. Cocokkan checksum dan fingerprint sertifikat publik di docs/release-certificate.sha256.

Pindah dari APK uji perlu ekspor dokumen/TXT lalu instal ulang sekali. Folder/indeks OCR tidak ikut ekspor PDF, dan belum ada restore library/impor PDF. Uninstall menghapus data internal. Update setelah memakai jalur kunci penerbit harus mempertahankan package, kunci dan menaikkan versionCode.

Status pemeriksaan ada di [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md). Jangan menyamakan keberhasilan build dengan pengujian perangkat.
