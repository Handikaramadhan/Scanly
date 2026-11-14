package com.scanly.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scanly.app.data.AppPreferences
import com.scanly.app.data.Document
import com.scanly.app.data.DocumentRepository
import com.scanly.app.data.DocumentFolder
import com.scanly.app.data.FolderAppearance
import com.scanly.app.data.OcrText
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import com.scanly.app.data.ThemeMode
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class ScannerPhase { IDLE, PREPARING, OPEN, TOOLS }
data class OcrProgress(val documentId: String,val completed: Int=0,val total: Int=0,val cancelling: Boolean=false)
data class HomeState(
    val documents: List<Document> = emptyList(),
    val folders: List<DocumentFolder> = emptyList(),
    val textMatches: Set<String> = emptySet(),
    val searchQuery: String = "",
    val searching: Boolean = false,
    val ocrProgress: OcrProgress? = null,
    val textDocumentId: String? = null,
    val recognizedText: OcrText? = null,
    val loadingText: Boolean = false,
    val operationRunning: Boolean = false,
    val scannerPhase: ScannerPhase = ScannerPhase.IDLE,
    val status: String? = null,
    val message: String? = null,
    val theme: ThemeMode = ThemeMode.SYSTEM
) {
    val busy: Boolean get() = operationRunning || scannerPhase != ScannerPhase.IDLE || ocrProgress!=null
    val progressText: String get() = when (scannerPhase) {
        ScannerPhase.PREPARING -> "Menyiapkan scanner Google…"
        ScannerPhase.TOOLS -> "Alat dokumen sedang dibuka…"
        ScannerPhase.OPEN -> "Pemindaian sedang berlangsung…"
        ScannerPhase.IDLE -> status ?: "Memuat dokumen…"
    }
}
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    val repository = DocumentRepository(application)
    private val preferences = AppPreferences(application)
    private val mutable = MutableStateFlow(HomeState(theme = preferences.theme()))
    private val operationMutex = Mutex()
    private var pendingOperations = 0
    private var ocrJob: Job?=null
    private var searchJob: Job?=null
    private var textJob: Job?=null
    val state = mutable.asStateFlow()
    init { refresh() }

    fun message(value: String) { mutable.update { it.copy(message = value) } }
    fun dismissMessage(expected: String) { mutable.update { if (it.message == expected) it.copy(message = null) else it } }
    fun scannerBusy() { mutable.update { it.copy(scannerPhase = ScannerPhase.PREPARING) } }
    fun toolsReady() { mutable.update { it.copy(scannerPhase=ScannerPhase.TOOLS) } }
    fun scannerReady() { mutable.update { it.copy(scannerPhase = ScannerPhase.OPEN) } }
    fun scannerIdle() { mutable.update { it.copy(scannerPhase = ScannerPhase.IDLE) } }
    fun setTheme(theme: ThemeMode) { preferences.setTheme(theme); mutable.update { it.copy(theme = theme) } }
    fun refresh() = runOperation("Memuat dokumen…") {}
    fun save(uri: Uri,folderId: String?=null) = runOperation("Menyimpan PDF…", "PDF berhasil disimpan.") { repository.save(uri,folderId) }
    fun rename(document: Document, name: String) = runOperation("Mengubah nama…", "Nama dokumen berhasil diubah.") { repository.rename(document, name) }
    fun delete(document: Document) = runOperation("Menghapus dokumen…", "Dokumen dihapus.") { repository.delete(document) }
    fun export(documentId: String, uri: Uri) = runOperation("Mengekspor PDF…", "Salinan PDF berhasil diekspor.") { repository.export(documentId, uri) }

    fun exportText(documentId: String,uri: Uri)=runOperation("Mengekspor teks…","Teks berhasil diekspor.") { repository.exportText(documentId,uri) }
    fun createFolder(name: String, color: String = FolderAppearance.DEFAULT_COLOR,
                     icon: String = FolderAppearance.DEFAULT_ICON) = runOperation("Membuat folder…", "Folder dibuat.") {
        repository.createFolder(name, color, icon)
    }
    fun updateFolder(id: String, name: String, color: String, icon: String) = runOperation("Menyimpan folder…", "Folder diperbarui.") {
        repository.updateFolder(id, name, color, icon)
    }
    fun renameFolder(id: String,name: String)=runOperation("Mengubah folder…","Nama folder diubah.") { repository.renameFolder(id,name) }
    fun deleteFolder(id: String)=runOperation("Menghapus folder…","Folder dihapus. PDF dipertahankan di Tanpa folder.") { repository.deleteFolder(id) }
    fun move(document: Document,folderId: String?)=runOperation("Memindahkan dokumen…","Folder dokumen diubah.") { repository.move(document,folderId) }
    fun search(query: String) {
        searchJob?.cancel()
        val term=query.trim()
        mutable.update { it.copy(searchQuery=term,textMatches=emptySet(),searching=term.isNotEmpty()) }
        if(term.isEmpty()) return
        searchJob=viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            try {
                delay(250)
                val matches=repository.searchText(term)
                mutable.update { if(it.searchQuery==term) it.copy(textMatches=matches,searching=false) else it }
            } catch(cancelled: CancellationException) { throw cancelled }
            catch(_: Exception) { mutable.update { it.copy(searching=false) };message("Pencarian isi belum berhasil. Pencarian nama tetap tersedia.") }
        }
    }
    fun showText(document: Document) {
        textJob?.cancel()
        mutable.update { it.copy(textDocumentId=document.id,recognizedText=null,loadingText=true) }
        textJob=viewModelScope.launch {
            try { val text=repository.readOcr(document.id);mutable.update { it.copy(recognizedText=text,loadingText=false) } }
            catch(cancelled: CancellationException) { throw cancelled }
            catch(_: Exception) { mutable.update { it.copy(loadingText=false) };message("Teks belum dapat dibuka. Jalankan OCR lagi.") }
        }
    }
    fun closeText() { textJob?.cancel();mutable.update { it.copy(textDocumentId=null,recognizedText=null,loadingText=false) } }
    fun recognize(document: Document,engine: com.scanly.app.ocr.PdfOcrEngine=com.scanly.app.ocr.PdfOcrEngine()) {
        if(state.value.busy) return
        mutable.update { it.copy(ocrProgress=OcrProgress(document.id)) }
        ocrJob=viewModelScope.launch {
            try {
                repository.recognize(document.id,engine) { completed,total -> mutable.update { it.copy(ocrProgress=OcrProgress(document.id,completed,total)) } }
                val documents=repository.list()
                mutable.update { it.copy(documents=documents) }
                search(state.value.searchQuery)
                showText(documents.first { it.id==document.id })
                message("OCR selesai. Periksa hasil sebelum menyalin teks.")
            } catch(cancelled: CancellationException) { message("OCR dibatalkan. PDF dan hasil OCR yang sudah tersimpan tetap aman.");throw cancelled }
            catch(_: Exception) { message("OCR belum berhasil. PDF tetap tersimpan. Coba lagi dengan gambar yang lebih jelas.") }
            finally { mutable.update { it.copy(ocrProgress=null) } }
        }
    }
    fun cancelOcr() { mutable.update { it.copy(ocrProgress=it.ocrProgress?.copy(cancelling=true)) };ocrJob?.cancel() }

    /** Scanner/export callbacks must queue behind an initial load, never be silently dropped. */
    private fun runOperation(status: String, success: String? = null, operation: suspend () -> Unit) {
        pendingOperations++
        mutable.update { it.copy(operationRunning = true) }
        viewModelScope.launch {
            try {
                operationMutex.withLock {
                    mutable.update { it.copy(status = status) }
                    operation()
                    try {
                        val documents = repository.list()
                        val folders=repository.folders()
                        mutable.update { it.copy(documents=documents,folders=folders,message=success ?: it.message) }
                        if(state.value.searchQuery.isNotEmpty()) search(state.value.searchQuery)
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) {
                        message((success?.let { "$it " } ?: "") + "Daftar belum dapat dimuat. Tarik daftar ke bawah untuk memuat ulang.")
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: IllegalArgumentException) { message(error.message ?: "Dokumen atau nama berkas tidak valid.") }
            catch (_: SecurityException) { message("Akses berkas ditolak. Pilih ulang lokasi ekspor atau coba lagi.") }
            catch (_: IOException) { message("Operasi gagal. Periksa ruang penyimpanan dan berkas asal, lalu coba lagi. Salinan ekspor yang gagal mungkin belum lengkap.") }
            catch (_: Exception) { message("Operasi belum berhasil. Silakan coba lagi.") }
            finally {
                pendingOperations--
                mutable.update { it.copy(operationRunning = pendingOperations > 0, status = if (pendingOperations == 0) null else it.status) }
            }
        }
    }
}
