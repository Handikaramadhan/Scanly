# Scanly 1.9.0

Warna fitur Beranda dan daftar Folder dengan warna/ikon yang dapat dipilih, memakai kunci penerbit yang sama dengan 1.7.1; Play Store ditunda. Ikuti [GITHUB_RELEASE.md](GITHUB_RELEASE.md) untuk build, signing, aset publikasi dan identitas update.

APK utama: **Scanly-1.9.0.apk**. Jangan distribusikan APK unsigned atau artifact debug CI sebagai pembaruan jalur rilis ini. Cocokkan checksum dan fingerprint sertifikat publik di docs/release-certificate.sha256.

Pengguna 1.7.1 dapat memasang pembaruan tanpa uninstall. Pindah dari APK uji 1.7.0/sebelumnya perlu ekspor dokumen/TXT lalu instal ulang sekali. Folder/indeks OCR tidak ikut ekspor PDF, dan belum ada restore library/impor PDF. Uninstall menghapus data internal. Update setelah memakai jalur kunci penerbit harus mempertahankan package, kunci dan menaikkan versionCode.

Status pemeriksaan ada di [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md). Jangan menyamakan keberhasilan build dengan pengujian perangkat.
