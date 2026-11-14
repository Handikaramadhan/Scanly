# Layout Scanly 1.8

Mengikuti wireframe pemilik di Untitled-2026-11-03-1835.png. Gambar asli tetap di workspace; implementasi memakai Compose native dan tema terang/gelap yang sudah ada.

- Beranda: judul Scanly, slogan dan penjelasan singkat, alat 2×2, pemisah, lalu tiga dokumen terbaru. Scan Dokumen membuka Google; JPG ke PDF menerima gambar. PDF ke JPG/PNG dan Sematkan tanda tangan membuka halaman pemilih PDF yang dapat dicari.
- Dokumen: pencarian di atas seluruh PDF, pengurutan dan kartu dokumen ringkas. Ketuk kartu untuk buka; menu tiga titik memuat operasi dokumen. Tidak ada chip atau pengelolaan folder di atas daftar.
- Folder: tab tersendiri, tombol Buat folder dan kartu 2 kolom dengan ilustrasi folder, nama, jumlah dokumen dan menu rename/hapus. Tanpa folder adalah kategori bawaan. Ketuk kartu untuk membuka isi; scan di dalamnya menyimpan ke folder terpilih.
- Pengaturan: tema dan informasi aplikasi tetap tersedia sebagai tab keempat.

Kembali dari isi folder menuju grid, dari pemilih PDF menuju Beranda. Pencarian folder tidak memfilter tab Dokumen. Posisi folder dipulihkan saat Activity dibuat ulang. Tarik untuk refresh tetap tersedia.

Folder visual tetap memakai katalog lokal yang sama; tidak ada migrasi direktori PDF atau perubahan signing identity. APK 1.8.0 adalah pembaruan untuk APK penerbit 1.7.1.

Screenshot sintetis hasil tes lokal:

- [Beranda](screenshots/home-layout.png)
- [Folder](screenshots/folders-grid.png)
- [Dokumen](screenshots/documents-search.png)
- [320 dp dan font 150%](screenshots/home-small-large-font.png)

Screenshot bukan foto layar HP. Keyboard, TalkBack dan native scanner/renderer tetap mengikuti DEVICE_TESTS.md.
