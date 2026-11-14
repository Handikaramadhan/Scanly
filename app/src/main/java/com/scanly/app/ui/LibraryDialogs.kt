package com.scanly.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.scanly.app.data.Document
import com.scanly.app.data.DocumentFolder

@Composable
internal fun MoveFolderDialog(document: Document,folders: List<DocumentFolder>,onDismiss: ()->Unit,onMove: (String?)->Unit) {
    AlertDialog(onDismissRequest=onDismiss,title={ Text("Pindahkan ke folder") },text={
        Column(Modifier.fillMaxWidth().heightIn(max=360.dp).verticalScroll(rememberScrollState())) {
            Text(document.name,style=MaterialTheme.typography.bodyMedium,modifier=Modifier.padding(bottom=8.dp))
            TextButton(onClick={ onMove(null) },modifier=Modifier.fillMaxWidth().semantics { contentDescription="Pilih Tanpa folder" }) {
                Text("Tanpa folder",Modifier.weight(1f));if(document.folderId==null) Icon(Icons.Outlined.Check,"Folder saat ini")
            }
            folders.forEach { folder -> TextButton(onClick={ onMove(folder.id) },modifier=Modifier.fillMaxWidth().semantics { contentDescription="Pilih folder ${folder.name}" }) {
                FolderSymbol(folder, Modifier.size(28.dp)); Spacer(Modifier.width(12.dp))
                Text(folder.name,Modifier.weight(1f));if(document.folderId==folder.id) Icon(Icons.Outlined.Check,"Folder saat ini")
            } }
            if(folders.isEmpty()) Text("Buat folder baru di tab Folder.",style=MaterialTheme.typography.bodySmall)
        }
    },confirmButton={ TextButton(onClick=onDismiss) { Text("Tutup") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OcrTextDialog(state: HomeState,document: Document?,onDismiss: ()->Unit,onRecognize: (Document)->Unit,onExportText: (Document)->Unit,onMessage: (String)->Unit) {
    val context=LocalContext.current
    val text=state.recognizedText
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                TopAppBar(title={ Text("Teks dokumen") },navigationIcon={ TextButton(onClick=onDismiss) { Text("Tutup") } })
                Text(document?.name ?: "Hasil OCR",Modifier.padding(horizontal=20.dp,vertical=8.dp),style=MaterialTheme.typography.titleMedium)
                Text("Periksa hasil OCR. Pencarian teks tersedia di Scanly; PDF asli tidak diubah.",Modifier.padding(horizontal=20.dp),
                    style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                when {
                    state.loadingText -> LinearProgressIndicator(Modifier.fillMaxWidth().padding(20.dp))
                    text==null -> Text("Belum ada hasil OCR. Jalankan Baca teks untuk dokumen ini.",Modifier.weight(1f).padding(20.dp))
                    text.text.isBlank() -> Text("Tidak ada teks yang terbaca. Coba dokumen yang lebih terang dan tajam.",Modifier.weight(1f).padding(20.dp))
                    else -> LazyColumn(Modifier.weight(1f).fillMaxWidth(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
                        itemsIndexed(text.pages) { index,page -> SelectionContainer {
                            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                Text("Halaman ${index+1}",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
                                Text(page.ifBlank { "Tidak ada teks pada halaman ini." },style=MaterialTheme.typography.bodyLarge)
                            }
                        } }
                    }
                }
                if(!state.loadingText) Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    TextButton(modifier=Modifier.weight(1f),enabled=text!=null && text.text.isNotBlank(),onClick={
                        if(text!!.text.length>200_000) onMessage("Teks panjang. Gunakan Ekspor TXT untuk seluruh hasil.")
                        else try {
                            val clipboard=context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText(document?.name ?: "Teks dokumen",text.text))
                            onMessage("Teks disalin.")
                        } catch(_: Exception) { onMessage("Teks belum dapat disalin. Gunakan Ekspor TXT.") }
                    }) { Text("Salin semua") }
                    TextButton(modifier=Modifier.weight(1f),enabled=text!=null && text.text.isNotBlank(),onClick={
                        if(text!!.text.length>200_000) onMessage("Teks panjang. Gunakan Ekspor TXT untuk seluruh hasil.")
                        else try { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type="text/plain";putExtra(Intent.EXTRA_TEXT,text!!.text);putExtra(Intent.EXTRA_SUBJECT,document?.name) },"Bagikan teks")) }
                        catch(_: Exception) { onMessage("Aplikasi berbagi teks tidak tersedia.") }
                    }) { Text("Bagikan teks") }
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    TextButton(modifier=Modifier.weight(1f),enabled=document!=null && text!=null && !state.busy,onClick={ document?.let(onExportText) }) { Text("Ekspor TXT") }
                    TextButton(modifier=Modifier.weight(1f),enabled=document!=null && !state.busy,onClick={ document?.let { onDismiss();onRecognize(it) } }) { Text("OCR ulang") }
                    }
                }
            }
        }
    }
}
