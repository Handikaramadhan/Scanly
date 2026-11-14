package com.scanly.app.data

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class DocumentRepository(private val context: Context) {
    // Directory creation and PDF inspection happen inside the IO operations, not ViewModel construction.
    private val store by lazy { PdfStore(File(context.filesDir, "documents")) { file -> pageCount(file) } }
    private val catalog=DocumentCatalog(context.applicationContext)
    private data class CachedPages(val length: Long, val modified: Long, val pages: Int?)
    private val pageCache = ConcurrentHashMap<String, CachedPages>()

    private fun pageCount(file: File): Int = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
        PdfRenderer(descriptor).use { renderer ->
            if (renderer.pageCount < 1) throw IOException("PDF tidak memiliki halaman.")
            renderer.pageCount
        }
    }

    suspend fun list(): List<Document> = withContext(Dispatchers.IO) {
        synchronized(catalog.lock) {
        catalog.recover(File(context.filesDir,"documents"))
        val files = store.list()
        val metadata=catalog.synchronize(files)
        pageCache.keys.retainAll(files.map { it.name }.toSet())
        files.map { file ->
            val cached = pageCache[file.name]?.takeIf { it.length == file.length() && it.modified == file.lastModified() }
            val pages = cached ?: CachedPages(file.length(), file.lastModified(), runCatching { pageCount(file) }.getOrNull())
                .also { pageCache[file.name] = it }
            val entry=metadata[file.name]
            Document(file,pages.pages,entry?.folderId,entry?.ocrPages,entry?.ocrAt)
        }
        }
    }
    suspend fun save(uri: Uri,folderId: String? = null) = withContext(Dispatchers.IO) {
        val destinationStore = store
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Hasil scan tidak dapat dibaca.")
        synchronized(catalog.lock) {
            val file=destinationStore.save(input)
            catalog.synchronize(store.list())
            if(folderId!=null && catalog.folders().any { it.id==folderId }) catalog.move(file.name,folderId)
            file
        }
    }
    suspend fun rename(document: Document,name: String): File = withContext(Dispatchers.IO) {
        synchronized(catalog.lock) {
            val source=store.resolve(document.id)
            require(DocumentName.error(name)==null) { DocumentName.error(name) ?: "Nama tidak valid." }
            val newName=DocumentName.normalize(name)+".pdf"
            if(source.name==newName) return@synchronized source
            catalog.recover(File(context.filesDir,"documents"));catalog.synchronize(store.list())
            catalog.prepareRename(source.name,newName)
            val renamed=try { store.rename(source,name) } catch(error: Exception) { catalog.cancelRename(source.name);throw error }
            // If SQLite fails here, the durable journal completes metadata rename on next load.
            catalog.finishRename(source.name,renamed.name)
            pageCache.remove(source.name)
            renamed
        }
    }
    suspend fun delete(document: Document) = withContext(Dispatchers.IO) {
        synchronized(catalog.lock) { store.delete(document.file);catalog.remove(document.id);Unit }
    }
    suspend fun folders() = withContext(Dispatchers.IO) { catalog.folders() }
    suspend fun createFolder(name: String, color: String = FolderAppearance.DEFAULT_COLOR,
                             icon: String = FolderAppearance.DEFAULT_ICON) = withContext(Dispatchers.IO) { catalog.createFolder(name, color, icon) }
    suspend fun updateFolder(id: String, name: String, color: String, icon: String) = withContext(Dispatchers.IO) {
        catalog.updateFolder(id, name, color, icon)
    }
    suspend fun renameFolder(id: String,name: String) = withContext(Dispatchers.IO) { catalog.renameFolder(id,name) }
    suspend fun deleteFolder(id: String) = withContext(Dispatchers.IO) { catalog.deleteFolder(id) }
    suspend fun move(document: Document,folderId: String?) = withContext(Dispatchers.IO) {
        synchronized(catalog.lock) { store.resolve(document.id);catalog.synchronize(store.list());catalog.move(document.id,folderId) }
    }
    suspend fun readOcr(id: String): OcrText? = withContext(Dispatchers.IO) {
        synchronized(catalog.lock) { store.resolve(id);catalog.synchronize(store.list());catalog.readOcr(id) }
    }
    suspend fun searchText(query: String) = withContext(Dispatchers.IO) { catalog.searchText(query) }
    suspend fun recognize(id: String,engine: com.scanly.app.ocr.PdfOcrEngine = com.scanly.app.ocr.PdfOcrEngine(),progress: (Int,Int)->Unit) {
        val file=withContext(Dispatchers.IO) { synchronized(catalog.lock) { catalog.synchronize(store.list());store.resolve(id) } }
        val size=file.length();val modified=file.lastModified()
        val pages=engine.recognize(file,progress)
        kotlinx.coroutines.currentCoroutineContext().ensureActive()
        withContext(Dispatchers.IO) { synchronized(catalog.lock) { catalog.saveOcr(file,pages,size,modified) } }
    }
    suspend fun exportText(id: String,uri: Uri) = withContext(Dispatchers.IO) {
        require(uri.authority!="${context.packageName}.files") { "Pilih lokasi ekspor di luar penyimpanan dokumen aplikasi." }
        val text=readOcr(id) ?: throw IllegalArgumentException("Jalankan OCR sebelum mengekspor teks.")
        require(uri.scheme!="file" || File(requireNotNull(uri.path)).canonicalFile.parentFile!=File(context.filesDir,"documents").canonicalFile) { "Pilih lokasi ekspor di luar dokumen asli." }
        val stream=context.contentResolver.openOutputStream(uri,"wt") ?: throw IOException("Lokasi ekspor tidak tersedia.")
        stream.bufferedWriter(Charsets.UTF_8).use { writer -> text.pages.forEachIndexed { index,page -> if(index>0) writer.write("\n\n");writer.write(page) } }
    }
    suspend fun ownedFile(id: String): File = withContext(Dispatchers.IO) { store.resolve(id) }
    suspend fun saveToolResult(sessionId: String,input: File,folderId: String?): File = withContext(Dispatchers.IO) {
        require(java.util.UUID.fromString(sessionId).toString()==sessionId)
        synchronized(catalog.lock) {
            val file=store.saveGenerated("Scan-$sessionId.pdf") { target -> input.inputStream().use { source -> target.outputStream().use { destination -> source.copyTo(destination);destination.fd.sync() } } }
            catalog.synchronize(store.list())
            if(folderId!=null && catalog.folders().any { it.id==folderId }) catalog.move(file.name,folderId)
            file
        }
    }
    fun uri(document: Document): Uri {
        val file = store.requireOwned(document.file)
        require(file.isFile) { "Dokumen asal tidak tersedia." }
        return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    suspend fun export(documentId: String, destination: Uri) = withContext(Dispatchers.IO) {
        val source = store.resolve(documentId)
        require(destination.authority != "${context.packageName}.files" &&
            (destination.scheme != "file" || File(requireNotNull(destination.path)).canonicalFile != source.canonicalFile)) {
            "Pilih lokasi ekspor yang berbeda dari dokumen asal."
        }
        source.inputStream().use { input ->
            val stream = context.contentResolver.openOutputStream(destination, "wt")
                ?: throw IOException("Lokasi ekspor tidak dapat ditulis.")
            stream.use { input.copyTo(it) }
        }
    }
}
