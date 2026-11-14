package com.scanly.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scanly.app.data.Document
import com.scanly.app.data.DocumentFolder
import com.scanly.app.data.DocumentName
import com.scanly.app.data.DocumentSort
import com.scanly.app.tools.ToolMode
import java.text.Collator
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanlyApp(model: HomeViewModel, onScan: (String?) -> Unit, onOpen: (Document) -> Unit,
              onShare: (Document) -> Unit, onExport: (Document) -> Unit,
              onExportText: (Document) -> Unit = {}, onTools: (ToolMode, Document?) -> Unit = { _, _ -> }) {
    val state by model.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var selectedFolder by rememberSaveable { mutableStateOf("*") }
    var query by rememberSaveable { mutableStateOf("") }
    var folderQuery by rememberSaveable { mutableStateOf("") }
    var folderEditor by rememberSaveable { mutableStateOf(false) }
    var editingFolderId by rememberSaveable { mutableStateOf<String?>(null) }
    var folderMenu by remember { mutableStateOf(false) }
    var deleteFolder by rememberSaveable { mutableStateOf<String?>(null) }
    var moveDocument by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var renameId by rememberSaveable { mutableStateOf<String?>(null) }
    var renameText by rememberSaveable { mutableStateOf("") }
    var sortName by rememberSaveable { mutableStateOf(DocumentSort.NEWEST.name) }
    var sortMenu by remember { mutableStateOf(false) }
    var toolPicker by rememberSaveable { mutableStateOf<String?>(null) }
    val folderOpen = tab == 2 && selectedFolder != "*"
    val folder = state.folders.find { it.id == selectedFolder }
    val folderRoot = tab == 2 && !folderOpen
    val activeQuery = if (folderOpen) folderQuery else if (tab == 1) query else ""
    val refreshing = state.operationRunning && state.status == "Memuat dokumen…"
    val requestScan = { onScan(if (folderOpen) folder?.id else null) }
    val sort = DocumentSort.valueOf(sortName)
    val collator = remember { Collator.getInstance(Locale.forLanguageTag("id-ID")) }
    val homeList = rememberLazyListState()
    val documentsList = rememberLazyListState()
    val foldersList = rememberLazyListState()
    val folderDocumentsList = rememberLazyListState()
    val closeFolder = { selectedFolder = "*"; folderQuery = "" }
    BackHandler(enabled = folderEditor || toolPicker != null || tab != 0) {
        if (folderEditor) folderEditor = false else if (toolPicker != null) toolPicker = null else if (folderOpen) closeFolder() else tab = 0
    }
    LaunchedEffect(state.folders, state.operationRunning) {
        if (!state.operationRunning) {
            if (selectedFolder != "*" && selectedFolder != "~" && folder == null) closeFolder()
            if (editingFolderId != null && state.folders.none { it.id == editingFolderId }) folderEditor = false
        }
    }
    LaunchedEffect(activeQuery) { model.search(activeQuery) }
    LaunchedEffect(query, sortName) { documentsList.scrollToItem(0) }
    LaunchedEffect(folderQuery, selectedFolder, sortName) { folderDocumentsList.scrollToItem(0) }
    val matches = remember(state.documents, state.textMatches, state.searchQuery, activeQuery, sortName, tab, selectedFolder) {
        val filtered = state.documents.filter {
            (!folderOpen || (selectedFolder == "~" && it.folderId == null) || it.folderId == selectedFolder) &&
                (it.name.contains(activeQuery.trim(), ignoreCase = true) ||
                    (state.searchQuery == activeQuery.trim() && it.id in state.textMatches))
        }
        when (sort) {
            DocumentSort.NEWEST -> filtered.sortedWith(compareByDescending<Document> { it.modified }.thenBy { it.id })
            DocumentSort.OLDEST -> filtered.sortedWith(compareBy<Document> { it.modified }.thenBy { it.id })
            DocumentSort.NAME -> filtered.sortedWith { a, b -> collator.compare(a.name, b.name) }
        }
    }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it, withDismissAction = true, duration = SnackbarDuration.Long)
            model.dismissMessage(it)
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text(if (folderEditor) { if (editingFolderId == null) "Buat folder" else "Edit folder" }
                else if (toolPicker != null) "Pilih PDF" else when (tab) {
                    1 -> "Dokumen"
                    2 -> if (folderOpen) folder?.name ?: "Tanpa folder" else "Folder"
                    3 -> "Pengaturan"
                    else -> "Scanly"
                }, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
            }, navigationIcon = {
                if (folderEditor) IconButton(onClick = { folderEditor = false }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Tutup editor folder")
                } else if (toolPicker != null) IconButton(onClick = { toolPicker = null }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Kembali ke Beranda")
                } else if (folderOpen) IconButton(onClick = closeFolder) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Kembali ke folder")
                }
            }, actions = {
                if (!folderEditor && toolPicker == null && folderOpen && folder != null) Box {
                    IconButton(enabled = !state.busy, onClick = { folderMenu = true }) {
                        Icon(Icons.Outlined.MoreVert, "Opsi folder")
                    }
                    FolderMenu(folderMenu, { folderMenu = false }, {
                        folderMenu = false; editingFolderId = selectedFolder; folderEditor = true
                    }, { folderMenu = false; deleteFolder = selectedFolder })
                }
            })
        },
        snackbarHost = { SnackbarHost(snackbar, Modifier.padding(bottom = if (folderEditor) 80.dp else 0.dp)) },
        bottomBar = {
            if (!folderEditor) NavigationBar {
                listOf("Beranda" to Icons.Outlined.Home, "Dokumen" to Icons.Outlined.Description,
                    "Folder" to Icons.Outlined.Folder, "Pengaturan" to Icons.Outlined.Settings).forEachIndexed { index, item ->
                    NavigationBarItem(selected = tab == index, onClick = { focusManager.clearFocus(); toolPicker = null; tab = index },
                        icon = { Icon(item.second, contentDescription = null) }, label = { Text(item.first, maxLines = 1, overflow = TextOverflow.Ellipsis) })
                }
            }
        },
        floatingActionButton = {
            if (!folderEditor && toolPicker == null && (tab == 1 || folderOpen) && !state.busy) ExtendedFloatingActionButton(
                onClick = requestScan, icon = { Icon(Icons.Outlined.DocumentScanner, null) },
                text = { Text("Scan Dokumen") })
        }
    ) { padding ->
        if (folderEditor) {
            FolderEditorScreen(state.folders.find { it.id == editingFolderId }, state.folders, !state.busy,
                modifier = Modifier.padding(padding), onSave = { name, color, icon ->
                    editingFolderId?.let { model.updateFolder(it, name, color, icon) }
                        ?: model.createFolder(name, color, icon)
                    folderEditor = false
                })
        } else if (toolPicker != null) {
            val mode = ToolMode.valueOf(toolPicker!!)
            ToolDocumentPicker(mode, state.documents, !state.busy,
                modifier = Modifier.padding(padding), onDismiss = { toolPicker = null },
                onPick = {
                    if (toolPicker != null) { toolPicker = null; onTools(mode, it) }
                },
                onScan = { toolPicker = null; onScan(null) })
        } else if (tab == 3) SettingsScreen(state.theme, model::setTheme, Modifier.padding(padding))
        else PullToRefreshBox(isRefreshing = refreshing,
            onRefresh = { if (!state.busy) model.refresh() },
            modifier = Modifier.fillMaxSize().padding(padding).testTag("document-refresh").semantics {
                customActions = listOf(CustomAccessibilityAction("Muat ulang dokumen") {
                    if (!state.busy) { model.refresh(); true } else false
                })
            }) {
            LazyColumn(
                state = when { tab == 0 -> homeList; folderRoot -> foldersList; folderOpen -> folderDocumentsList; else -> documentsList },
                modifier = Modifier.fillMaxSize().testTag(if (folderRoot) "folder-list" else "document-list"),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp,
                    bottom = if (tab == 1 || folderOpen) 104.dp else 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (tab == 0) {
                    item {
                        Text("Dari kertas, jadi rapi", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text("Scan dan simpan dokumen di perangkatmu", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(20.dp))
                        HomeTools(!state.busy, requestScan,
                            { toolPicker = ToolMode.PDF_TO_IMAGES.name },
                            { onTools(ToolMode.IMAGES_TO_PDF, null) },
                            { toolPicker = ToolMode.SIGN_PDF.name })
                        Spacer(Modifier.height(20.dp))
                        HorizontalDivider()
                    }
                    item {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Dokumen Baru", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold)
                            if (state.documents.isNotEmpty()) TextButton(onClick = { tab = 1 }) { Text("Lihat semua") }
                        }
                    }
                }
                if (tab == 1 || folderOpen) item {
                    DocumentSearch(activeQuery, {
                        if (folderOpen) folderQuery = it else query = it
                    }, { focusManager.clearFocus() })
                    if (state.searching) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (folderOpen) "${matches.size} dokumen di folder ini" else "Semua dokumen (${matches.size})",
                            Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                        Box {
                            TextButton(onClick = { sortMenu = true }) {
                                Icon(Icons.AutoMirrored.Outlined.Sort, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp)); Text(sort.label)
                            }
                            DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                                DocumentSort.entries.forEach { option ->
                                    DropdownMenuItem(text = { Text(option.label) }, onClick = {
                                        sortName = option.name; sortMenu = false
                                    }, leadingIcon = { if (option == sort) Icon(Icons.Outlined.Check, null) })
                                }
                            }
                        }
                    }
                }
                if (folderRoot) {
                    item {
                        Text("Ruang untuk dokumenmu", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text("Buka folder untuk melihat dan mengatur isinya.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        FilledTonalButton(enabled = !state.busy, onClick = { editingFolderId = null; folderEditor = true },
                            modifier = Modifier.semantics { contentDescription = "Buat folder" }) {
                            Icon(Icons.Outlined.CreateNewFolder, null)
                            Spacer(Modifier.width(8.dp)); Text("Buat folder")
                        }
                    }
                    val entries = listOf(DocumentFolder("~", "Tanpa folder")) + state.folders
                    items(entries, key = { it.id }) { entry ->
                        FolderRow(entry, state.documents.count {
                            if (entry.id == "~") it.folderId == null else it.folderId == entry.id
                        }, !state.busy, onOpen = { selectedFolder = entry.id; folderQuery = "" },
                            onRename = { editingFolderId = entry.id; folderEditor = true },
                            onDelete = { deleteFolder = entry.id })
                    }
                }
                state.ocrProgress?.let { progress -> item {
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Membaca teks dokumen…", style = MaterialTheme.typography.titleMedium)
                            if (progress.total > 0) {
                                LinearProgressIndicator(progress = { progress.completed.toFloat() / progress.total }, modifier = Modifier.fillMaxWidth())
                                Text("${progress.completed} dari ${progress.total} halaman")
                            } else LinearProgressIndicator(Modifier.fillMaxWidth())
                            TextButton(enabled = !progress.cancelling, onClick = model::cancelOcr) {
                                Text(if (progress.cancelling) "Membatalkan…" else "Batalkan OCR")
                            }
                        }
                    }
                } }
                if (state.busy && !refreshing && state.ocrProgress == null) item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text(state.progressText, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (!folderRoot) {
                    val shown = if (tab == 0) state.documents.sortedByDescending { it.modified }.take(3) else matches
                    if (shown.isEmpty() && !state.busy && !state.searching) item {
                        DocumentEmptyState(folderOpen, activeQuery.isNotBlank(), requestScan,
                            onClear = { if (folderOpen) folderQuery = "" else query = "" })
                    }
                    items(shown, key = { it.id }) { document ->
                        DocumentCard(document, !state.busy, { onOpen(document) }, { onShare(document) },
                            { onExport(document) }, { deleteId = document.id }, {
                                renameId = document.id; renameText = document.name
                            }, folderName = state.folders.find { it.id == document.folderId }?.name,
                            onMove = { moveDocument = document.id }, onOcr = { model.recognize(document) },
                            onText = { model.showText(document) },
                            onPictures = { onTools(ToolMode.PDF_TO_IMAGES, document) },
                            onSign = { onTools(ToolMode.SIGN_PDF, document) })
                    }
                }
            }
        }
    }
    state.folders.find { it.id==deleteFolder }?.let { folder -> AlertDialog(onDismissRequest={ deleteFolder=null },
        title={ Text("Hapus folder?") },text={ Text("Hapus ${folder.name}? Semua PDF di dalamnya tetap tersimpan di Tanpa folder.") },
        confirmButton={ TextButton(enabled=!state.busy,onClick={ deleteFolder=null;model.deleteFolder(folder.id) }) { Text("Hapus folder") } },
        dismissButton={ TextButton(onClick={ deleteFolder=null }) { Text("Batal") } }) }
    state.documents.find { it.id==moveDocument }?.let { document -> MoveFolderDialog(document,state.folders,
        onDismiss={ moveDocument=null },onMove={ moveDocument=null;model.move(document,it) }) }
    if(state.textDocumentId!=null) OcrTextDialog(state,state.documents.find { it.id==state.textDocumentId },
        model::closeText,model::recognize,onExportText,model::message)
    state.documents.find { it.id == renameId }?.let { document ->
        val normalized = DocumentName.normalize(renameText)
        val nameError = DocumentName.error(renameText) ?: if (state.documents.any {
            it.id != document.id && it.name.equals(normalized, ignoreCase = true)
        }) "Nama ini sudah dipakai. Pilih nama lain." else null
        AlertDialog(
            onDismissRequest = { if (!state.busy) renameId = null },
            title = { Text("Ubah nama dokumen") },
            text = {
                OutlinedTextField(
                    value = renameText, onValueChange = { renameText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nama dokumen") }, singleLine = true,
                    enabled = !state.busy, isError = nameError != null,
                    supportingText = { Text(nameError ?: "Ekstensi .pdf ditambahkan otomatis.") }
                )
            },
            confirmButton = {
                TextButton(enabled = !state.busy && nameError == null, onClick = {
                    model.rename(document, renameText)
                    renameId = null
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(enabled = !state.busy, onClick = { renameId = null }) { Text("Batal") } }
        )
    }
    state.documents.find { it.id == deleteId }?.let { document ->
        AlertDialog(onDismissRequest = { deleteId = null }, title = { Text("Hapus dokumen?") },
            text = { Text("Hapus ${document.name}? Salinan lokal ini akan dihapus permanen. Salinan yang sudah diekspor tidak ikut dihapus.") },
            confirmButton = { TextButton(enabled = !state.busy, onClick = { deleteId = null; model.delete(document) }) { Text("Hapus") } },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Batal") } })
    }
}

@Composable
private fun DocumentCard(document: Document, enabled: Boolean, onOpen: () -> Unit, onShare: () -> Unit,
                         onExport: () -> Unit, onDelete: () -> Unit, onRename: () -> Unit,
                         folderName: String?, onMove: () -> Unit, onOcr: () -> Unit, onText: () -> Unit,
                         onPictures: () -> Unit, onSign: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    OutlinedCard(onClick = onOpen, enabled = enabled, modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.PictureAsPdf, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(document.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val pages = document.pageCount?.let { "$it halaman · " } ?: "PDF tidak dapat dibaca · "
                Text(pages + formatSize(document.size), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                folderName?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                if (document.ocrPages != null) Text("OCR tersedia", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box {
                IconButton(enabled = enabled, onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "Opsi ${document.name}") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Buka PDF") }, onClick = { menu = false; onOpen() })
                    DropdownMenuItem(text = { Text("Bagikan") }, onClick = { menu = false; onShare() })
                    DropdownMenuItem(text = { Text("Ekspor JPG/PNG") }, enabled = document.pageCount != null, onClick = { menu = false; onPictures() })
                    DropdownMenuItem(text = { Text("Tambah tanda tangan") }, enabled = document.pageCount != null, onClick = { menu = false; onSign() })
                    DropdownMenuItem(text = { Text(if (document.ocrPages == null) "Baca teks (OCR)" else "Jalankan ulang OCR") },
                        enabled = document.pageCount != null, onClick = { menu = false; onOcr() })
                    DropdownMenuItem(text = { Text("Lihat teks") }, enabled = document.ocrPages != null, onClick = { menu = false; onText() })
                    DropdownMenuItem(text = { Text("Pindahkan ke folder") }, onClick = { menu = false; onMove() })
                    DropdownMenuItem(text = { Text("Ubah nama") }, onClick = { menu = false; onRename() })
                    DropdownMenuItem(text = { Text("Ekspor salinan PDF") }, onClick = { menu = false; onExport() })
                    DropdownMenuItem(text = { Text("Hapus") }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}
private fun formatSize(bytes: Long): String = if (bytes < 1024 * 1024) "${(bytes / 1024).coerceAtLeast(1)} KB"
    else String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
