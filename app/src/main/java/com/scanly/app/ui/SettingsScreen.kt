package com.scanly.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import com.scanly.app.BuildConfig
import com.scanly.app.data.ThemeMode
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext

@Composable
fun SettingsScreen(theme: ThemeMode, onThemeChange: (ThemeMode) -> Unit, modifier: Modifier = Modifier) {
    var info by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Sederhana. Lokal. Terbuka.", style = MaterialTheme.typography.headlineMedium)
        Text("Scanly ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyLarge)
        Text("Tampilan", style = MaterialTheme.typography.titleMedium)
        OutlinedCard {
            ThemeMode.entries.forEach { mode ->
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    .selectable(selected = theme == mode, role = Role.RadioButton, onClick = { onThemeChange(mode) })
                    .padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = theme == mode, onClick = null)
                    Spacer(Modifier.width(12.dp))
                    Text(mode.label)
                }
            }
        }
        OutlinedCard {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Simpan salinan dokumen penting", style = MaterialTheme.typography.titleMedium)
                Text("PDF disimpan di penyimpanan internal aplikasi. Menghapus aplikasi atau datanya dapat menghapus semua PDF. Gunakan menu Ekspor salinan PDF untuk menyimpan di lokasi pilihanmu.")
                Text("Backup otomatis dokumen dinonaktifkan. Lokasi ekspor dan aplikasi tujuan berbagi dapat memiliki kebijakan cloud sendiri.")
            }
        }
        Text("Kamera, deteksi tepi dan editor menggunakan scanner Google. Komponen mungkin perlu diunduh saat pertama dipakai; setelah siap, pemindaian dapat berjalan offline. Tampilan dan pengambilan foto mengikuti scanner Google.")
        Text("Folder dan hasil OCR disimpan lokal. OCR huruf Latin berjalan di perangkat; ekspor TXT untuk menyimpan teks secara terpisah. Ekspor PDF tidak menyertakan folder atau indeks OCR aplikasi.")
        Text("Alat gambar/PDF dan tanda tangan berjalan lokal. Hasil TTD berupa salinan PDF gambar; file asli tetap tersedia. Gambar dan coretan sesi disimpan sementara di cache privat.")
        TextButton(onClick = { info = "Kebijakan privasi" }) { Text("Baca kebijakan privasi") }
        TextButton(onClick = { info = "Lisensi open source" }) { Text("Lisensi dan komponen pihak ketiga") }
    }
    info?.let { title ->
        var text by remember(title) { mutableStateOf("Memuat…") }
        LaunchedEffect(title) {
            text = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching { context.assets.open(if (title == "Kebijakan privasi") "privacy.txt" else "licenses.txt").bufferedReader().use { it.readText() } }
                    .getOrDefault("Informasi tidak dapat dimuat.")
            }
        }
        AlertDialog(onDismissRequest = { info = null }, title = { Text(title) },
            text = { Text(text, Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(onClick = { info = null }) { Text("Tutup") } })
    }
}
