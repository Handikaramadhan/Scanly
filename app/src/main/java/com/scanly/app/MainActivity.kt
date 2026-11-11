package com.scanly.app

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.scanly.app.data.Document
import com.scanly.app.scanner.ScannerLauncher
import com.scanly.app.tools.DocumentToolsActivity
import com.scanly.app.ui.ScannerPhase
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.scanly.app.ui.HomeViewModel
import com.scanly.app.ui.ScanlyApp
import com.scanly.app.ui.theme.ScanlyTheme

class MainActivity : ComponentActivity() {
    private val model: HomeViewModel by viewModels()
    private val scanner by lazy { ScannerLauncher() }
    private var pendingTextExportId: String?=null
    private var pendingScanFolderId: String?=null
    private var pendingExportId: String? = null
    private val scanResult = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        handleScannerResult(result.resultCode,result.data)
    }
    internal fun handleScannerResult(resultCode: Int,data: Intent?) {
        val folder=pendingScanFolderId;pendingScanFolderId=null
        model.scannerIdle()
        if(resultCode==Activity.RESULT_OK) {
            val pdf=runCatching { GmsDocumentScanningResult.fromActivityResultIntent(data)?.pdf }.getOrNull()
            if(pdf==null) model.message("Scanner tidak menghasilkan PDF. Silakan coba lagi.")
            else model.save(pdf.uri,folder)
        }
    }
    private val exportResult = registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val documentId = pendingExportId
        pendingExportId = null
        if (uri != null) {
            if (documentId != null) model.export(documentId, uri)
            else model.message("Dokumen asal tidak tersedia. Silakan ulangi ekspor.")
        }
    }
    private val textExportResult=registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        val id=pendingTextExportId;pendingTextExportId=null
        if(uri!=null && id!=null) model.exportText(id,uri)
    }
    private val toolsResult=registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        model.scannerIdle()
        if(result.resultCode==Activity.RESULT_OK) { model.refresh();model.message("PDF hasil berhasil disimpan.") }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingExportId = savedInstanceState?.getString("exportId")
        pendingScanFolderId=savedInstanceState?.getString("scanFolderId")
        pendingTextExportId=savedInstanceState?.getString("textExportId")
        if(savedInstanceState!=null && model.state.value.scannerPhase==ScannerPhase.PREPARING) model.scannerIdle()
        enableEdgeToEdge()
        setContent {
            val state by model.state.collectAsStateWithLifecycle()
            com.scanly.app.ui.theme.MatchSystemBars(this, state.theme)
            ScanlyTheme(state.theme) {
                ScanlyApp(model, onScan = { folderId ->
                    if (!model.state.value.busy) {
                        pendingScanFolderId=folderId
                        model.scannerBusy()
                        scanner.launch(this,scanResult,onReady={ model.scannerReady() },onError={
                            model.scannerIdle();model.message(it)
                        })
                    }
                }, onOpen = { openOrShare(it, false) }, onShare = { openOrShare(it, true) }, onExport = {
                    pendingExportId = it.id
                    try { exportResult.launch(it.file.name) }
                    catch (_: ActivityNotFoundException) { pendingExportId = null; model.message("Pemilih lokasi penyimpanan tidak tersedia di perangkat ini.") }
                },onExportText={ document ->
                    pendingTextExportId=document.id
                    try { textExportResult.launch(document.name+".txt") }
                    catch(_: ActivityNotFoundException) { pendingTextExportId=null;model.message("Pemilih lokasi penyimpanan tidak tersedia.") }
                },onTools={ mode,document ->
                    if(!model.state.value.busy) {
                        try {
                            model.toolsReady()
                            toolsResult.launch(Intent(this,DocumentToolsActivity::class.java).putExtra("mode",mode.name).putExtra("documentId",document?.id).putExtra("folderId",document?.folderId))
                        } catch(_: Exception) { model.scannerIdle();model.message("Alat dokumen belum dapat dibuka.") }
                    }
                })
            }
        }
    }
    override fun onStop() {
        super.onStop()
        if(model.state.value.scannerPhase==ScannerPhase.PREPARING) model.scannerIdle()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("exportId", pendingExportId)
        outState.putString("scanFolderId",pendingScanFolderId)
        outState.putString("textExportId",pendingTextExportId)
        super.onSaveInstanceState(outState)
    }
    private fun openOrShare(document: Document, share: Boolean) {
        try {
            val uri = model.repository.uri(document)
            val intent = if (share) Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"; putExtra(Intent.EXTRA_STREAM, uri)
            } else Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.clipData = ClipData.newRawUri(document.name, uri)
            startActivity(if (share) Intent.createChooser(intent, "Bagikan PDF") else intent)
        } catch (_: ActivityNotFoundException) {
            model.message("Belum ada aplikasi penampil PDF. Ekspor atau bagikan PDF untuk membukanya di aplikasi lain.")
        } catch (_: Exception) { model.message("Dokumen tidak dapat dibuka. Periksa apakah berkas masih tersedia.") }
    }
}
