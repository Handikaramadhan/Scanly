package com.scanly.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.activity.compose.BackHandler
import com.scanly.app.data.DocumentSort
import java.text.Collator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scanly.app.data.Document
import com.scanly.app.tools.ToolMode
import com.scanly.app.data.DocumentName
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanlyApp(model: HomeViewModel, onScan: (String?) -> Unit, onOpen: (Document) -> Unit,
              onShare: (Document) -> Unit, onExport: (Document) -> Unit, onExportText: (Document) -> Unit = {},onTools: (ToolMode,Document?)->Unit = { _,_ -> }) {
    val state by model.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    var selectedFolder by rememberSaveable { mutableStateOf("*") }
    var folderEditor by rememberSaveable { mutableStateOf(false) }
    var editingFolderId by rememberSaveable { mutableStateOf<String?>(null) }
    var folderMenu by remember { mutableStateOf(false) }
    var deleteFolder by rememberSaveable { mutableStateOf<String?>(null) }
    var moveDocument by rememberSaveable { mutableStateOf<String?>(null) }
    val refreshing = state.operationRunning && state.status == "Memuat dokumen…"
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val requestScan={ onScan(if(tab==1) selectedFolder.takeUnless { it=="*" || it=="~" } else null) }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var renameId by rememberSaveable { mutableStateOf<String?>(null) }
    var renameText by rememberSaveable { mutableStateOf("") }
    var sortName by rememberSaveable { mutableStateOf(DocumentSort.NEWEST.name) }
    var sortMenu by remember { mutableStateOf(false) }
    val sort = DocumentSort.valueOf(sortName)
    val collator = remember { Collator.getInstance(Locale.forLanguageTag("id-ID")) }
    val homeList = rememberLazyListState()
    val documentsList = rememberLazyListState()
    BackHandler(enabled=tab!=0) { if(tab==1 && selectedFolder!="*") selectedFolder="*" else tab=0 }
    LaunchedEffect(state.folders) { if(selectedFolder!="*" && selectedFolder!="~" && state.folders.none { it.id==selectedFolder }) selectedFolder="*" }
    LaunchedEffect(query) { model.search(query) }
    LaunchedEffect(query, sortName) { documentsList.scrollToItem(0) }
    val matches = remember(state.documents,state.textMatches,query,sortName,selectedFolder) {
        val filtered=state.documents.filter {
            (selectedFolder=="*" || (selectedFolder=="~" && it.folderId==null) || it.folderId==selectedFolder) &&
                (it.name.contains(query.trim(),ignoreCase=true) || it.id in state.textMatches)
        }
        when (sort) {
            DocumentSort.NEWEST -> filtered.sortedWith(compareByDescending<Document> { it.modified }.thenBy { it.id })
            DocumentSort.OLDEST -> filtered.sortedWith(compareBy<Document> { it.modified }.thenBy { it.id })
            DocumentSort.NAME -> filtered.sortedWith { a, b -> collator.compare(a.name, b.name) }
        }
    }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it, withDismissAction = true, duration = SnackbarDuration.Long); model.dismissMessage(it) }
    }
    Scaffold(
        topBar = { TopAppBar(title = {
            Text(when (tab) { 2 -> "Pengaturan"; 1 -> "Dokumen"; else -> "Scanly" }, fontWeight = FontWeight.Bold)
        },actions={ IconButton(enabled=!state.busy,onClick={ onTools(ToolMode.IMAGES_TO_PDF,null) }) { Icon(Icons.Outlined.Collections,"Gambar ke PDF") } }) },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                listOf("Beranda" to Icons.Outlined.Home, "Dokumen" to Icons.Outlined.Description,
                    "Pengaturan" to Icons.Outlined.Settings).forEachIndexed { index, item ->
                    NavigationBarItem(selected = tab == index, onClick = { tab = index },
                        icon = { Icon(item.second, contentDescription = null) }, label = { Text(item.first) })
                }
            }
        },
        floatingActionButton = {
            if (tab != 2 && !state.busy) ExtendedFloatingActionButton(
                onClick = requestScan, icon = { Icon(Icons.Outlined.DocumentScanner, null) }, text = { Text("Scan Dokumen") })
        }
    ) { padding ->
        if (tab == 2) SettingsScreen(state.theme, model::setTheme, Modifier.padding(padding))
        else PullToRefreshBox(isRefreshing = refreshing,
            onRefresh = { if (!state.busy) model.refresh() },
            modifier = Modifier.fillMaxSize().padding(padding).testTag("document-refresh").semantics {
                customActions = listOf(CustomAccessibilityAction("Muat ulang dokumen") {
                    if (!state.busy) { model.refresh(); true } else false
                })
            }) {
        LazyColumn(state = if (tab == 0) homeList else documentsList, modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (tab == 0) item {
                Text("Dari kertas, jadi rapi.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Scan dan simpan dokumen di perangkatmu.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(20.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.PhonelinkLock, contentDescription = null)
                        Text("Dokumenmu, tetap milikmu.", style = MaterialTheme.typography.titleMedium)
                        Text("Tanpa akun, iklan, atau watermark. Scanner Google, folder dan OCR lokal untuk dokumenmu.")
                    }
                }
            }
            item {
                Text(if(tab==0) "Dokumen terbaru" else when(selectedFolder) {
                    "*" -> "Semua dokumen (${state.documents.size})"
                    "~" -> "Tanpa folder"
                    else -> state.folders.find { it.id==selectedFolder }?.name ?: "Dokumen"
                },
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }
            if(tab==1) item {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("Folder",Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
                    IconButton(enabled=!state.busy,onClick={ editingFolderId=null;folderEditor=true }) { Icon(Icons.Outlined.CreateNewFolder,"Buat folder") }
                    Box {
                        IconButton(enabled=!state.busy && state.folders.any { it.id==selectedFolder },onClick={ folderMenu=true }) { Icon(Icons.Outlined.MoreVert,"Opsi folder") }
                        DropdownMenu(expanded=folderMenu,onDismissRequest={ folderMenu=false }) {
                            DropdownMenuItem(text={ Text("Ubah nama folder") },onClick={ folderMenu=false;editingFolderId=selectedFolder;folderEditor=true })
                            DropdownMenuItem(text={ Text("Hapus folder") },onClick={ folderMenu=false;deleteFolder=selectedFolder })
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected=selectedFolder=="*",onClick={ selectedFolder="*" },label={ Text("Semua") })
                    FilterChip(selected=selectedFolder=="~",onClick={ selectedFolder="~" },label={ Text("Tanpa folder") })
                    state.folders.forEach { folder -> FilterChip(selected=selectedFolder==folder.id,onClick={ selectedFolder=folder.id },label={ Text(folder.name) }) }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("Cari nama atau isi dokumen") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Hapus pencarian")
                        }
                    },
                    supportingText = { Text("Pencarian isi tersedia setelah Baca teks (OCR).") }
                )
                if(state.searching) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top=8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("${matches.size} dokumen", style = MaterialTheme.typography.labelLarge)
                    Box {
                        TextButton(onClick = { sortMenu = true }) {
                            Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(sort.label)
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
            state.ocrProgress?.let { progress -> item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text("Membaca teks dokumen…",style=MaterialTheme.typography.titleMedium)
                        if(progress.total>0) {
                            LinearProgressIndicator(progress={ progress.completed.toFloat()/progress.total },modifier=Modifier.fillMaxWidth())
                            Text("${progress.completed} dari ${progress.total} halaman")
                        } else LinearProgressIndicator(Modifier.fillMaxWidth())
                        TextButton(enabled=!progress.cancelling,onClick=model::cancelOcr) { Text(if(progress.cancelling) "Membatalkan…" else "Batalkan OCR") }
                    }
                }
            } }
            if (state.busy && !refreshing && state.ocrProgress==null) item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(state.progressText, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (state.documents.isEmpty() && !state.busy) item {
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Outlined.DocumentScanner, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Mulai dari satu halaman", style = MaterialTheme.typography.titleLarge)
                        Text("Nota, catatan, atau surat penting. Jadikan PDF tanpa watermark.")
                        Button(onClick = requestScan) { Text("Scan Dokumen Pertama") }
                    }
                }
            }
            if (tab == 1 && state.documents.isNotEmpty() && matches.isEmpty() && !state.busy && !state.searching) item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if(query.isBlank() && selectedFolder!="*") "Folder ini masih kosong" else "Dokumen tidak ditemukan", style = MaterialTheme.typography.titleMedium)
                    Text("Coba nama lain atau hapus pencarian untuk melihat semua dokumen.")
                    TextButton(onClick = { query="";selectedFolder="*" }) { Text("Tampilkan semua dokumen") }
                }
            }
            val shown = if (tab == 0) state.documents.take(5) else matches
            items(shown, key = { it.id }) { document ->
                DocumentCard(document, !state.busy, { onOpen(document) }, { onShare(document) },
                    { onExport(document) }, { deleteId = document.id }, {
                        renameId = document.id
                        renameText = document.name
                    },folderName=state.folders.find { it.id==document.folderId }?.name,
                    onMove={ moveDocument=document.id },onOcr={ model.recognize(document) },onText={ model.showText(document) },
                    onPictures={ onTools(ToolMode.PDF_TO_IMAGES,document) },onSign={ onTools(ToolMode.SIGN_PDF,document) })
            }
            if (tab == 0 && state.documents.size > 5) item {
                TextButton(onClick = { tab = 1 }) { Text("Lihat semua dokumen") }
            }
        }
        }
    }
    if(folderEditor) FolderNameDialog(if(editingFolderId==null) "Buat folder" else "Ubah nama folder",
        state.folders.find { it.id==editingFolderId }?.name ?: "",state.folders,editingFolderId,
        onDismiss={ folderEditor=false },onSave={ name ->
            folderEditor=false
            editingFolderId?.let { model.renameFolder(it,name) } ?: model.createFolder(name)
        })
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
                         folderName: String?,onMove: ()->Unit,onOcr: ()->Unit,onText: ()->Unit,onPictures: ()->Unit,onSign: ()->Unit) {
    var menu by remember { mutableStateOf(false) }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.PictureAsPdf, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(document.name, style = MaterialTheme.typography.titleSmall)
                folderName?.let { Text(it,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary) }
                document.ocrPages?.let { Text("OCR tersedia · $it halaman",style=MaterialTheme.typography.labelSmall) }
                val pages = document.pageCount?.let { "$it halaman · " } ?: "PDF tidak dapat dibaca · "
                Text(pages + formatSize(document.size), style = MaterialTheme.typography.bodySmall)
                Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(document.modified)),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box {
                IconButton(enabled = enabled, onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "Opsi ${document.name}") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text={ Text("Ekspor JPG/PNG") },enabled=document.pageCount!=null,onClick={ menu=false;onPictures() })
                    DropdownMenuItem(text={ Text("Tambah tanda tangan") },enabled=document.pageCount!=null,onClick={ menu=false;onSign() })
                    DropdownMenuItem(text={ Text(if(document.ocrPages==null) "Baca teks (OCR)" else "Jalankan ulang OCR") },enabled=document.pageCount!=null,onClick={ menu=false;onOcr() })
                    DropdownMenuItem(text={ Text("Lihat teks") },enabled=document.ocrPages!=null,onClick={ menu=false;onText() })
                    DropdownMenuItem(text={ Text("Pindahkan ke folder") },onClick={ menu=false;onMove() })
                    DropdownMenuItem(text = { Text("Ubah nama") }, onClick = { menu = false; onRename() })
                    DropdownMenuItem(text = { Text("Ekspor salinan PDF") }, onClick = { menu = false; onExport() })
                    DropdownMenuItem(text = { Text("Hapus") }, onClick = { menu = false; onDelete() })
                }
            }
        }
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            TextButton(enabled = enabled, onClick = onOpen) { Text("Buka PDF") }
            TextButton(enabled = enabled, onClick = onShare) { Text("Bagikan") }
            if(document.ocrPages!=null) TextButton(enabled=enabled,onClick=onText) { Text("Teks") }
        }
    }
}
private fun formatSize(bytes: Long): String = if (bytes < 1024 * 1024) "${(bytes / 1024).coerceAtLeast(1)} KB"
    else String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0))
