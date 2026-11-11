# Distribusi melalui GitHub

Repository: https://github.com/Handikaramadhan/Scanly

Git menyimpan source, lisensi, dokumentasi dan workflow. APK, ZIP source dan checksum didistribusikan sebagai **Release assets**, bukan dimasukkan ke riwayat Git. Tidak perlu akun Play Console untuk jalur ini; APK tetap perlu sertifikat signing yang konsisten agar Android menerima pembaruan.

## Kandidat awal 1.7.0

Berkas yang telah disiapkan lokal:

- artifacts/Scanly-1.7.0-release-test.apk — kandidat optimized, key uji lokal yang sama dengan versi percobaan lama.
- artifacts/Scanly-1.7.0-source.zip — source publik dengan allowlist, tanpa cache/SDK/key.
- artifacts/SHA256SUMS.txt — checksum kandidat dan artefak pendamping.
- artifacts/validation-summary.json — bukti tes/lint/pemeriksaan binary dan batasnya.

Kandidat ini bukan APK yang ditandatangani key produksi. Untuk percobaan publik, beri label **Pre-release** dan jelaskan batas uji perangkat. Sebelum distribusi stabil luas, pilih dan simpan key produksi privat; jangan mencampur APK dengan sertifikat berbeda sebagai jalur update yang sama. Jangan unggah keystore/password ke repository, issue, artifact atau log.

## Membuat Release

1. Buka https://github.com/Handikaramadhan/Scanly/releases/new dan pilih main sebagai target.
2. Gunakan tag v1.7.0-rc1, judul Scanly 1.7.0 Preview, dan isi catatan dari RELEASE_NOTES.md.
3. Lampirkan keempat berkas di atas. Jangan lampirkan keystore, release-signing.properties atau data scan.
4. Tandai Pre-release. Cocokkan SHA256SUMS dengan berkas yang benar-benar diunggah. Publish ketika paket dan catatannya telah ditinjau.
5. Sesudah asset publik tersedia, perbarui README dengan link APK aktual; jangan memasang link latest/download yang belum ada.

Untuk mengunggah melalui CLI yang sudah login:

```sh
gh release create v1.7.0-rc1 --repo Handikaramadhan/Scanly --target main --prerelease --title 'Scanly 1.7.0 Preview' --notes-file docs/RELEASE_NOTES.md artifacts/Scanly-1.7.0-release-test.apk artifacts/Scanly-1.7.0-source.zip artifacts/SHA256SUMS.txt artifacts/validation-summary.json
```

Jalankan perintah dari root proyek setelah verifikasi SHA-256 dan review. Workflow CI tidak mempublikasikan rilis sendiri.

## CI

.github/workflows/android.yml menjalankan tes/lint/build JDK 21 + SDK 36, membangun kandidat unsigned/source dan mengunggah artifact pemeriksaan. Token job hanya membaca contents; credential checkout tidak dipertahankan. Tidak ada key signing rilis yang disertakan.

APK debug dari runner berbeda key dengan APK lokal dan bisa berubah antar-build. Artifact CI untuk pengujian/kajian, bukan jalur update pengguna. Gunakan key yang sama dan proses signing terpisah untuk APK distribusi.

## Batas kandidat

61 tes lokal lulus dan lint tanpa error pada paket lokal awal. Kamera Google, akurasi OCR, picker/renderer perangkat dan latar TTD nyata masih perlu uji HP. Penghapus latar paling cocok tinta di kertas. TTD berupa salinan PDF gambar; sumber tetap tersedia. Lihat DEVICE_TESTS.md.
