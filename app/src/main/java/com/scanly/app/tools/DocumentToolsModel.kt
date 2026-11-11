package com.scanly.app.tools

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.scanly.app.data.DocumentRepository
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ToolState(val mode: ToolMode=ToolMode.IMAGES_TO_PDF,val loading: Boolean=true,val busy: Boolean=false,
    val images: List<File> = emptyList(),val source: File?=null,val page: Int=0,val pages: Int=0,
    val signature: File?=null,val rawSignature: File?=null,val position: SignaturePosition=SignaturePosition(),
    val strength: Int=10,val progress: String?=null,val message: String?=null,val completedId: String?=null)

class DocumentToolsModel(app: Application,private val saved: SavedStateHandle): AndroidViewModel(app) {
    private val repository=DocumentRepository(app)
    private val mutable=MutableStateFlow(ToolState());val state=mutable.asStateFlow()
    private val id=saved.get<String>("toolId") ?: UUID.randomUUID().toString().also { saved["toolId"]=it }
    init { require(UUID.fromString(id).toString()==id) }
    private val directory=File(app.cacheDir,"document-tool-$id").apply { mkdirs() }
    private var job: Job?=null
    private val deferred=java.util.ArrayDeque<()->Unit>()
    private var initialized=false
    private var folderId: String?=null
    fun initialize(mode: ToolMode,documentId: String?,folder: String?) {
        if(initialized) return;initialized=true;folderId=folder
        mutable.update { it.copy(mode=mode) }
        execute {
            val committed=File(getApplication<Application>().filesDir,"documents/Scan-$id.pdf")
            if(mode!=ToolMode.PDF_TO_IMAGES && committed.isFile) { mutable.update { it.copy(completedId=committed.name) };return@execute }
            val source=if(mode==ToolMode.IMAGES_TO_PDF) null else repository.ownedFile(requireNotNull(documentId))
            val count=if(source==null) 0 else withContext(Dispatchers.IO) { PdfPages(source).use { it.count } }
            val images=saved.get<ArrayList<String>>("images").orEmpty().map { owned(it) }.filter { it.isFile }
            val signature=saved.get<String>("signature")?.let(::owned)?.takeIf { it.isFile }
            val raw=saved.get<String>("rawSignature")?.let(::owned)?.takeIf { it.isFile }
            val page=(saved.get<Int>("page") ?: 0).coerceIn(0,maxOf(0,count-1))
            val pos=saved.get<FloatArray>("position")?.let { SignaturePosition(page,it[0],it[1],it[2]) } ?: SignaturePosition(page)
            mutable.update { it.copy(source=source,pages=count,page=page,images=images,signature=signature,rawSignature=raw,position=pos,strength=saved["strength"] ?: 10) }
        }
    }
    private fun owned(name: String): File { require(name==File(name).name);return File(directory,name) }
    fun message(text: String) { mutable.update { it.copy(message=text) } }
    fun dismissMessage() { mutable.update { it.copy(message=null) } }
    fun selectPage(delta: Int) {
        if(state.value.busy) return
        val page=(state.value.page+delta).coerceIn(0,maxOf(0,state.value.pages-1));saved["page"]=page
        val position=state.value.position.copy(page=page);savePosition(position)
        mutable.update { it.copy(page=page,position=position) }
    }
    fun place(position: SignaturePosition) { if(!state.value.busy) { savePosition(position);mutable.update { it.copy(position=position) } } }
    private fun savePosition(position: SignaturePosition) { saved["position"]=floatArrayOf(position.x,position.y,position.width) }
    fun importImages(uris: List<Uri>) { if(uris.isEmpty()) return;enqueue {
        for(uri in uris) {
            currentCoroutineContext().ensureActive()
            val target=owned("${UUID.randomUUID()}.png")
            try { withContext(Dispatchers.IO) { DocumentTools.importImage(getApplication(),uri,target) } }
            catch(error: Throwable) { target.delete();throw error }
            val files=state.value.images+target;saved["images"]=ArrayList(files.map { it.name });mutable.update { it.copy(images=files) }
        }
    } }
    fun moveImage(index: Int,delta: Int) {
        if(state.value.busy) return
        val target=index+delta;if(target !in state.value.images.indices) return
        val images=state.value.images.toMutableList().apply { add(target,removeAt(index)) }
        saved["images"]=ArrayList(images.map { it.name });mutable.update { it.copy(images=images) }
    }
    fun removeImage(index: Int) { if(!state.value.busy) {
        val images=state.value.images.toMutableList();images.removeAt(index).delete()
        saved["images"]=ArrayList(images.map { it.name });mutable.update { it.copy(images=images) }
    } }
    fun importSignature(uri: Uri) = enqueue {
        val raw=owned("ink-source-${UUID.randomUUID()}.png")
        withContext(Dispatchers.IO) { DocumentTools.importImage(getApplication(),uri,raw,1200) }
        saved["rawSignature"]=raw.name;saved["signature"]=null;mutable.update { it.copy(rawSignature=raw,signature=null) };cleanSignature(raw,state.value.strength)
    }
    fun setStrength(value: Int) { saved["strength"]=value;mutable.update { it.copy(strength=value) } }
    fun reclean() { state.value.rawSignature?.let { raw -> execute { saved["signature"]=null;mutable.update { it.copy(signature=null) };cleanSignature(raw,state.value.strength) } } }
    private suspend fun cleanSignature(raw: File,strength: Int) {
        val target=owned("ink-${UUID.randomUUID()}.png")
        withContext(Dispatchers.Default) {
            val bitmap=BitmapFactory.decodeFile(raw.path) ?: throw IOException("Gambar tanda tangan tidak tersedia.")
            try { val clean=SignatureCleaner.clean(bitmap,strength);try { DocumentTools.writeBitmap(clean,target,Bitmap.CompressFormat.PNG) } finally { clean.recycle() } } finally { bitmap.recycle() }
        }
        saved["signature"]=target.name;mutable.update { it.copy(signature=target) }
    }
    fun drawnSignature(bitmap: Bitmap)=enqueue {
        val target=owned("ink-drawn-${UUID.randomUUID()}.png")
        try { withContext(Dispatchers.IO) {
            val trimmed=SignatureCleaner.clean(bitmap)
            try { DocumentTools.writeBitmap(trimmed,target,Bitmap.CompressFormat.PNG) } finally { trimmed.recycle() }
        } } finally { bitmap.recycle() }
        saved["signature"]=target.name;saved["rawSignature"]=null;mutable.update { it.copy(signature=target,rawSignature=null) }
    }
    fun createPdf()=execute {
        val snapshot=state.value;val output=owned("result.pdf")
        withContext(Dispatchers.IO) {
            if(snapshot.mode==ToolMode.IMAGES_TO_PDF) DocumentTools.imagesToPdf(snapshot.images,output,directory,::progress)
            else DocumentTools.signPdf(requireNotNull(snapshot.source),requireNotNull(snapshot.signature),snapshot.position.copy(page=snapshot.page),output,directory,::progress)
        }
        currentCoroutineContext().ensureActive()
        val file=repository.saveToolResult(id,output,folderId);mutable.update { it.copy(completedId=file.name) }
    }
    fun export(uri: Uri,format: PictureFormat,page: Int?)=enqueue {
        require(uri.authority!="${getApplication<Application>().packageName}.files") { "Pilih lokasi di luar PDF asli." }
        require(uri.scheme!="file" || File(requireNotNull(uri.path)).canonicalFile.parentFile!=File(getApplication<Application>().filesDir,"documents").canonicalFile) { "Pilih lokasi di luar dokumen asli." }
        val source=requireNotNull(state.value.source)
        withContext(Dispatchers.IO) {
            (getApplication<Application>().contentResolver.openOutputStream(uri,"wt") ?: throw IOException("Lokasi ekspor tidak tersedia.")).use {
                DocumentTools.exportPictures(source,format,page,it,::progress)
            }
        }
        message("Gambar berhasil diekspor.")
    }
    private fun progress(done: Int,total: Int) { mutable.update { it.copy(progress="$done / $total halaman") } }
    fun cancel() { job?.cancel() }
    private fun enqueue(action: suspend ()->Unit) {
        if(state.value.busy || state.value.loading) deferred.addLast { enqueue(action) } else execute(action)
    }
    private fun execute(action: suspend ()->Unit) {
        if(state.value.busy) return
        mutable.update { it.copy(busy=true) }
        val next=viewModelScope.launch(start=CoroutineStart.LAZY) {
            try { action() }
            catch(cancelled: CancellationException) { message("Proses dibatalkan. Ekspor ke luar aplikasi yang terputus mungkin belum lengkap.");throw cancelled }
            catch(error: IllegalArgumentException) { message(error.message ?: "Periksa gambar dan posisi tanda tangan.") }
            catch(_: Exception) { message("Proses belum berhasil. PDF asli tetap aman. Periksa gambar/penyimpanan lalu coba lagi.") }
            finally {
                mutable.update { it.copy(busy=false,loading=false,progress=null) }
                if(viewModelScope.isActive && deferred.isNotEmpty()) deferred.removeFirst().invoke()
            }
        }
        job=next;next.start()
    }
    override fun onCleared() {
        deferred.clear();job?.cancel()
        val running=job
        if(running==null || running.isCompleted) directory.deleteRecursively()
        else running.invokeOnCompletion { directory.deleteRecursively() }
        super.onCleared()
    }
}
