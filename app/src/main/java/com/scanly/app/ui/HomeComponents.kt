package com.scanly.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.scanly.app.data.Document
import com.scanly.app.data.DocumentFolder
import com.scanly.app.tools.ToolMode

@Composable
internal fun HomeTools(enabled: Boolean, onScan: () -> Unit, onPictures: () -> Unit,
                       onImages: () -> Unit, onSign: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeTool("Scan Dokumen", "Scan Dokumen", Icons.Outlined.DocumentScanner, enabled, onScan, Modifier.weight(1f))
            HomeTool("PDF ke JPG/PNG", "PDF ke JPG/PNG", Icons.Outlined.Image, enabled, onPictures, Modifier.weight(1f), tone = 1)
        }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeTool("JPG ke PDF", "Gambar ke PDF", Icons.Outlined.Collections, enabled, onImages, Modifier.weight(1f), tone = 2)
            HomeTool("Sematkan tanda tangan", "Sematkan tanda tangan", Icons.Outlined.Draw, enabled, onSign, Modifier.weight(1f), tone = 3)
        }
    }
}

@Composable
private fun HomeTool(label: String, description: String, icon: ImageVector, enabled: Boolean,
                     onClick: () -> Unit, modifier: Modifier, tone: Int = 0) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < .5f
    val palettes = if (dark) listOf(
        Color(0xFF173665) to Color(0xFFBCD1FF), Color(0xFF49331A) to Color(0xFFFFCF93),
        Color(0xFF163E30) to Color(0xFFA5DFBF), Color(0xFF36234F) to Color(0xFFDDC3FF)
    ) else listOf(
        Color(0xFFE4ECFF) to Color(0xFF1645A0), Color(0xFFFFF0DD) to Color(0xFF874305),
        Color(0xFFE1F4EA) to Color(0xFF175D3F), Color(0xFFF0E7FF) to Color(0xFF603395)
    )
    val (background, foreground) = palettes[tone]
    OutlinedButton(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = background, contentColor = foreground,
            disabledContainerColor = background.copy(alpha = .45f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .38f)),
        border = BorderStroke(1.dp, foreground.copy(alpha = if (enabled) .2f else .08f)),
        modifier = modifier.fillMaxHeight().heightIn(min = 88.dp).semantics { contentDescription = description },
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 14.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, Modifier.size(24.dp))
            Text(label, minLines = 2, style = MaterialTheme.typography.labelLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
internal fun DocumentSearch(query: String, onQuery: (String) -> Unit, onSearch: () -> Unit) {
    OutlinedTextField(value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth(),
        singleLine = true, shape = RoundedCornerShape(12.dp), label = { Text("Cari nama atau isi dokumen") },
        leadingIcon = { Icon(Icons.Outlined.Search, null) },
        trailingIcon = {
            if (query.isNotEmpty()) IconButton(onClick = { onQuery("") }) { Icon(Icons.Outlined.Close, "Hapus pencarian") }
        }, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }))
}

@Composable
internal fun FolderMenu(expanded: Boolean, onDismiss: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit, onAppearance: () -> Unit = onRename) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(text = { Text("Ubah nama folder") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) }, onClick = onRename)
        DropdownMenuItem(text = { Text("Warna dan ikon") }, leadingIcon = { Icon(Icons.Outlined.Palette, null) }, onClick = onAppearance)
        DropdownMenuItem(text = { Text("Hapus folder") }, leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) }, onClick = onDelete)
    }
}

@Composable
internal fun FolderRow(folder: DocumentFolder, count: Int, enabled: Boolean, modifier: Modifier = Modifier,
                       onOpen: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    OutlinedCard(onClick = onOpen, enabled = enabled,
        modifier = modifier.fillMaxWidth().testTag("folder-${folder.id}"), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            FolderSymbol(folder, Modifier.size(44.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(folder.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("$count dokumen", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (folder.id != "~") Box {
                IconButton(onClick = { menu = true }, enabled = enabled) {
                    Icon(Icons.Outlined.MoreVert, "Opsi folder ${folder.name}")
                }
                FolderMenu(menu, { menu = false }, { menu = false; onRename() }, { menu = false; onDelete() })
            } else Spacer(Modifier.width(12.dp))
        }
    }
}

@Composable
internal fun DocumentEmptyState(inFolder: Boolean, searching: Boolean, onScan: () -> Unit, onClear: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(if (inFolder) Icons.Outlined.FolderOpen else Icons.Outlined.DocumentScanner, null,
                Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
            Text(when { searching -> "Dokumen tidak ditemukan"; inFolder -> "Folder ini masih kosong"; else -> "Mulai dari satu halaman" },
                style = MaterialTheme.typography.titleMedium)
            Text(when {
                searching -> "Coba nama lain atau cari isi dokumen yang sudah di-OCR."
                inFolder -> "Scan di sini atau pindahkan PDF melalui menu dokumen."
                else -> "Scan nota, catatan, atau surat. Hasilnya akan muncul di sini."
            }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (searching) TextButton(onClick = onClear) { Text("Hapus pencarian") }
            else TextButton(onClick = onScan) { Text(if (inFolder) "Scan ke folder ini" else "Scan Dokumen Pertama") }
        }
    }
}

@Composable
internal fun ToolDocumentPicker(mode: ToolMode, documents: List<Document>, enabled: Boolean,
                                modifier: Modifier = Modifier, onDismiss: () -> Unit,
                                onPick: (Document) -> Unit, onScan: () -> Unit) {
    var query by rememberSaveable(mode) { mutableStateOf("") }
    val readable = documents.filter { it.pageCount != null }
    val matches = readable.filter { it.name.contains(query.trim(), ignoreCase = true) }
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (mode == ToolMode.SIGN_PDF) "Pilih PDF untuk tanda tangan" else "Pilih PDF untuk ekspor gambar",
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        if (readable.isEmpty()) {
            Text("Belum ada PDF yang bisa diproses. Scan dokumen atau buat PDF dari gambar terlebih dahulu.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onScan, enabled = enabled) { Text("Scan Dokumen") }
            Spacer(Modifier.weight(1f))
        } else {
            OutlinedTextField(value = query, onValueChange = { query = it }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), label = { Text("Cari PDF") }, shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Outlined.Search, null) })
            if (matches.isEmpty()) Text("PDF tidak ditemukan.")
            LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(matches, key = { it.id }) { document ->
                    OutlinedCard(onClick = { onPick(document) }, enabled = enabled,
                        modifier = Modifier.fillMaxWidth().testTag("tool-document-${document.id}")) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.PictureAsPdf, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(document.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("${document.pageCount} halaman", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
        TextButton(onClick = onDismiss) { Text("Batal") }
    }
}
