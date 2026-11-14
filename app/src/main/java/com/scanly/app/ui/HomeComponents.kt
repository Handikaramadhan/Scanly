package com.scanly.app.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
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
            HomeTool("PDF ke JPG/PNG", "PDF ke JPG/PNG", Icons.Outlined.Image, enabled, onPictures, Modifier.weight(1f))
        }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeTool("JPG ke PDF", "Gambar ke PDF", Icons.Outlined.Collections, enabled, onImages, Modifier.weight(1f))
            HomeTool("Sematkan tanda tangan", "Sematkan tanda tangan", Icons.Outlined.Draw, enabled, onSign, Modifier.weight(1f))
        }
    }
}

@Composable
private fun HomeTool(label: String, description: String, icon: ImageVector, enabled: Boolean,
                     onClick: () -> Unit, modifier: Modifier) {
    OutlinedButton(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(12.dp),
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
internal fun FolderMenu(expanded: Boolean, onDismiss: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(text = { Text("Ubah nama folder") }, leadingIcon = { Icon(Icons.Outlined.Edit, null) }, onClick = onRename)
        DropdownMenuItem(text = { Text("Hapus folder") }, leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) }, onClick = onDelete)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FolderCard(folder: DocumentFolder, count: Int, enabled: Boolean, modifier: Modifier = Modifier,
                        onOpen: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    OutlinedCard(onClick = onOpen, enabled = enabled,
        modifier = modifier.testTag("folder-${folder.id}"), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Box(Modifier.fillMaxWidth().height(96.dp)) {
                FolderIllustration(Modifier.align(Alignment.Center).size(100.dp, 78.dp))
                if (folder.id != "~") Box(Modifier.align(Alignment.TopEnd)) {
                    IconButton(onClick = { menu = true }, enabled = enabled) {
                        Icon(Icons.Outlined.MoreVert, "Opsi folder ${folder.name}", Modifier.size(20.dp))
                    }
                    FolderMenu(menu, { menu = false }, { menu = false; onRename() }, { menu = false; onDelete() })
                }
            }
            Text(folder.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.heightIn(min = 40.dp))
            Text("$count dokumen", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Decorative folder silhouette: the tab and inset sheets make it readable without relying on colour. */
@Composable
private fun FolderIllustration(modifier: Modifier = Modifier) {
    val back = MaterialTheme.colorScheme.primary
    val front = MaterialTheme.colorScheme.primaryContainer
    val paper = MaterialTheme.colorScheme.surface
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val tab = Path().apply {
            moveTo(w * .05f, h * .18f)
            quadraticTo(w * .05f, h * .10f, w * .12f, h * .10f)
            lineTo(w * .37f, h * .10f); lineTo(w * .47f, h * .23f)
            lineTo(w * .88f, h * .23f)
            quadraticTo(w * .95f, h * .23f, w * .95f, h * .31f)
            lineTo(w * .95f, h * .86f); lineTo(w * .05f, h * .86f); close()
        }
        drawPath(tab, back)
        drawRoundRect(paper, Offset(w * .16f, h * .29f), Size(w * .68f, h * .45f), CornerRadius(w * .03f))
        drawRoundRect(front, Offset(w * .03f, h * .39f), Size(w * .94f, h * .51f), CornerRadius(w * .07f))
        drawRoundRect(back.copy(alpha = .25f), Offset(w * .15f, h * .71f), Size(w * .26f, h * .035f), CornerRadius(w * .02f))
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
