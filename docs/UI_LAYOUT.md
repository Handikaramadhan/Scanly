# Layout Scanly 1.9

Susunan Beranda/Dokumen mengikuti wireframe pemilik, dengan penyempurnaan warna dan folder sesuai permintaan berikutnya.

- Beranda: empat alat 2×2, berwarna biru (scan), jingga (PDF ke gambar), hijau (gambar ke PDF), dan ungu (tanda tangan). Warna menyesuaikan tema terang/gelap. Tiga dokumen terbaru tetap di bawahnya.
- Dokumen: seluruh PDF, pencarian/urut dan menu pengelolaan. Tidak ada chip folder.
- Folder: daftar satu kolom dengan ikon kecil, nama dan jumlah dokumen. Tidak ada ilustrasi thumbnail/grid. Ketuk baris untuk membuka isi; menu menyediakan rename, Warna dan ikon, serta hapus.
- Editor folder: nama, delapan warna pilihan, warna sendiri lewat HEX #RRGGBB dan sepuluh ikon. Warna biru/ikon folder dipakai otomatis bila tidak memilih. Gunakan tampilan bawaan mengembalikan keduanya.
- Pengaturan dan pemilih PDF tetap terpisah. Back dari isi folder ke daftar; Back dari editor membatalkan draft. Draft tetap ada setelah Activity dibuat ulang.

Ikon pilihan: folder, kerja, sekolah, tagihan, pribadi, rumah, kesehatan, perjalanan, foto, favorit. Tanpa folder adalah kategori sistem dan tidak dikustomisasi. Pilihan lokal tersimpan pada katalog versi 2; migrasi hanya menambah warna/ikon, tanpa memindahkan PDF.

Target sentuh pemilih warna/ikon minimal 48 dp. Warna terpilih memiliki outline dan centang; ikon terpilih memiliki outline dan latar. Warna custom memilih glyph hitam/putih menurut luminance agar ikon tetap terbaca. Tombol Simpan tetap terpisah dari daftar yang dapat digulir dan notifikasi.

Screenshot sintetis hasil tes lokal:

- [Beranda](screenshots/home-layout.png)
- [Daftar folder](screenshots/folders-list.png)
- [Editor warna/ikon](screenshots/folder-appearance-editor.png)
- [Beranda gelap](screenshots/home-colors-dark.png)
- [Daftar gelap](screenshots/folders-list-dark.png)
- [320 dp/font 150%](screenshots/home-small-large-font.png)

Screenshot bukan foto layar HP. Keyboard, TalkBack dan native scanner/renderer tetap mengikuti DEVICE_TESTS.md.
