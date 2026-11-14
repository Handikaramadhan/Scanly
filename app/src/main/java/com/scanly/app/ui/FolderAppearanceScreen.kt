package com.scanly.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.scanly.app.data.DocumentFolder
import com.scanly.app.data.FolderAppearance
import com.scanly.app.data.FolderIconKey

private val folderColors = listOf(
    "Biru" to "#275DCE", "Hijau" to "#16734B", "Jingga" to "#A7500B", "Ungu" to "#7041B6",
    "Merah" to "#B83240", "Mawar" to "#A53374", "Toska" to "#086C78", "Abu-abu" to "#5B6574"
)
private fun iconVector(id: String): ImageVector = when (id) {
    "work" -> Icons.Outlined.WorkOutline
    "school" -> Icons.Outlined.School
    "receipt" -> Icons.AutoMirrored.Outlined.ReceiptLong
    "person" -> Icons.Outlined.PersonOutline
    "home" -> Icons.Outlined.Home
    "health" -> Icons.Outlined.MedicalServices
    "travel" -> Icons.Outlined.FlightTakeoff
    "photo" -> Icons.Outlined.Photo
    "star" -> Icons.Outlined.StarOutline
    else -> Icons.Outlined.Folder
}
private fun folderColor(value: String) = Color(android.graphics.Color.parseColor(
    runCatching { FolderAppearance.color(value) }.getOrDefault(FolderAppearance.DEFAULT_COLOR)))
private fun onFolderColor(color: Color) = if (color.luminance() > .179f) Color.Black else Color.White

@Composable
internal fun FolderSymbol(folder: DocumentFolder, modifier: Modifier = Modifier) {
    val color = folderColor(folder.color)
    Surface(modifier, color = color, contentColor = onFolderColor(color), shape = RoundedCornerShape(10.dp)) {
        Box(contentAlignment = Alignment.Center) { Icon(iconVector(folder.icon), null, Modifier.size(24.dp)) }
    }
}

@Composable
internal fun FolderEditorScreen(folder: DocumentFolder?, folders: List<DocumentFolder>, enabled: Boolean,
                                modifier: Modifier = Modifier, onSave: (String, String, String) -> Unit) {
    var name by rememberSaveable(folder?.id) { mutableStateOf(folder?.name ?: "") }
    var color by rememberSaveable(folder?.id) { mutableStateOf(folder?.color ?: FolderAppearance.DEFAULT_COLOR) }
    var icon by rememberSaveable(folder?.id) { mutableStateOf(folder?.icon ?: FolderAppearance.DEFAULT_ICON) }
    var custom by rememberSaveable(folder?.id) { mutableStateOf(folderColors.none { it.second == color }) }
    val normalized = name.trim()
    val nameError = when {
        normalized.isBlank() -> "Isi nama folder."
        normalized.length > 60 || normalized.any { it.isISOControl() || it == '/' || it == '\\' } -> "Maksimal 60 karakter, tanpa garis miring atau karakter kontrol."
        folders.any { it.id != folder?.id && it.name.equals(normalized, ignoreCase = true) } -> "Nama folder sudah dipakai."
        else -> null
    }
    val validColor = runCatching { FolderAppearance.color(color) }.getOrNull()
    val preview = DocumentFolder("preview", normalized.ifBlank { "Folder baru" }, validColor ?: FolderAppearance.DEFAULT_COLOR, icon)
    Column(modifier.fillMaxSize().imePadding()) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("folder-editor"),
            contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FolderSymbol(preview, Modifier.size(48.dp))
                    Text(preview.name, style = MaterialTheme.typography.titleMedium, maxLines = 2,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
            }
            item {
                OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth().testTag("folder-name"),
                    enabled = enabled, singleLine = true, label = { Text("Nama folder") },
                    isError = nameError != null, supportingText = { Text(nameError ?: "Pilih warna dan ikon, atau gunakan bawaan.") })
            }
            item { Text("Warna folder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            items(folderColors.chunked(4).size) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    folderColors.chunked(4)[row].forEach { (label, value) ->
                        val selected = validColor == value
                        val swatch = folderColor(value)
                        OutlinedButton(onClick = { color = value; custom = false }, enabled = enabled,
                            modifier = Modifier.weight(1f).heightIn(min = 56.dp).semantics {
                                contentDescription = "Warna $label"; this.selected = selected
                            }, contentPadding = PaddingValues(6.dp), shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(if (selected) 2.dp else 1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                            Surface(color = swatch, contentColor = onFolderColor(swatch), shape = CircleShape, modifier = Modifier.size(32.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (selected) Icon(Icons.Outlined.Check, null, Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
            item {
                TextButton(onClick = { if (custom && validColor == null) color = FolderAppearance.DEFAULT_COLOR; custom = !custom }, enabled = enabled) { Text(if (custom) "Tutup kode warna" else "Warna lain (kode HEX)") }
                if (custom) OutlinedTextField(color, { color = it }, enabled = enabled,
                    modifier = Modifier.fillMaxWidth().testTag("folder-custom-color"), label = { Text("Kode warna") },
                    singleLine = true, isError = validColor == null,
                    supportingText = { Text(if (validColor == null) "Gunakan enam digit warna, misalnya #275DCE." else "Format #RRGGBB. Pratinjau berubah otomatis.") })
            }
            item { Text("Ikon folder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            items(FolderIconKey.entries.chunked(3).size) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val entries = FolderIconKey.entries.chunked(3)[row]
                    entries.forEach { option ->
                        val selected = icon == option.id
                        OutlinedButton(onClick = { icon = option.id }, enabled = enabled,
                            modifier = Modifier.weight(1f).heightIn(min = 80.dp).semantics {
                                contentDescription = "Ikon ${option.label}"; this.selected = selected
                            }, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
                            border = BorderStroke(if (selected) 2.dp else 1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(iconVector(option.id), null)
                                Text(option.label, style = MaterialTheme.typography.labelMedium, maxLines = 2,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }
                    }
                    repeat(3 - entries.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            item {
                TextButton(enabled = enabled, onClick = { color = FolderAppearance.DEFAULT_COLOR; icon = FolderAppearance.DEFAULT_ICON; custom = false }) {
                    Icon(Icons.Outlined.RestartAlt, null); Spacer(Modifier.width(8.dp)); Text("Gunakan tampilan bawaan")
                }
            }
        }
        Button(onClick = { onSave(normalized, validColor!!, icon) },
            enabled = enabled && nameError == null && validColor != null,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).heightIn(min = 48.dp)) {
            Text("Simpan folder")
        }
    }
}
