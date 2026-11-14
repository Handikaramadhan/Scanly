package com.scanly.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class DocumentFolder(val id: String, val name: String,
                          val color: String = FolderAppearance.DEFAULT_COLOR,
                          val icon: String = FolderAppearance.DEFAULT_ICON)
data class OcrText(val pages: List<String>,val completedAt: Long) {
    val text: String by lazy(LazyThreadSafetyMode.NONE) { pages.joinToString("\n\n") }
}
internal data class CatalogEntry(val folderId: String?,val ocrPages: Int?,val ocrAt: Long?)

/** PDFs stay in their original directory. SQLite holds only organisation and derived text. */
internal class DocumentCatalog(private val context: Context) {
    val lock: Any=locks.getOrPut(context.getDatabasePath("catalog.db").absolutePath) { Any() }
    private class Database(context: Context): SQLiteOpenHelper(context,"catalog.db",null,2) {
        override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE folders(id TEXT PRIMARY KEY,name TEXT NOT NULL,name_key TEXT NOT NULL UNIQUE,color TEXT NOT NULL DEFAULT '#275DCE',icon TEXT NOT NULL DEFAULT 'folder')")
            db.execSQL("CREATE TABLE documents(file_name TEXT PRIMARY KEY,folder_id TEXT REFERENCES folders(id) ON DELETE SET NULL,size INTEGER NOT NULL,modified INTEGER NOT NULL,ocr_pages INTEGER,ocr_at INTEGER)")
            db.execSQL("CREATE INDEX document_folder ON documents(folder_id)")
            db.execSQL("CREATE TABLE ocr_pages(file_name TEXT NOT NULL REFERENCES documents(file_name) ON UPDATE CASCADE ON DELETE CASCADE,page_index INTEGER NOT NULL,text TEXT NOT NULL,search_text TEXT NOT NULL,PRIMARY KEY(file_name,page_index))")
            db.execSQL("CREATE TABLE pending_rename(old_name TEXT PRIMARY KEY,new_name TEXT NOT NULL UNIQUE)")
        }
        override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {
            if (old == 1 && new == 2) {
                db.execSQL("ALTER TABLE folders ADD COLUMN color TEXT NOT NULL DEFAULT '#275DCE'")
                db.execSQL("ALTER TABLE folders ADD COLUMN icon TEXT NOT NULL DEFAULT 'folder'")
            } else throw IOException("Versi katalog belum didukung.")
        }
    }
    private fun <T> database(action: (SQLiteDatabase)->T): T = synchronized(lock) {
        Database(context).use { action(it.writableDatabase) }
    }
    private fun <T> transaction(db: SQLiteDatabase,action: ()->T): T {
        db.beginTransaction()
        try { val result=action();db.setTransactionSuccessful();return result } finally { db.endTransaction() }
    }
    fun recover(directory: File) = database { db -> transaction(db) { recoverIn(db,directory) } }
    private fun recoverIn(db: SQLiteDatabase,directory: File) {
        val pending=mutableListOf<Pair<String,String>>()
        db.rawQuery("SELECT old_name,new_name FROM pending_rename",null).use { c -> while(c.moveToNext()) pending.add(c.getString(0) to c.getString(1)) }
        pending.forEach { (old,new) ->
            require(old==File(old).name && new==File(new).name)
            if(!File(directory,old).exists() && File(directory,new).isFile) db.execSQL("UPDATE documents SET file_name=? WHERE file_name=?",arrayOf(new,old))
            db.delete("pending_rename","old_name=?",arrayOf(old))
        }
    }
    fun synchronize(files: List<File>): Map<String,CatalogEntry> = database { db -> transaction(db) {
        recoverIn(db,File(context.filesDir,"documents"))
        files.forEach { file ->
            val values=ContentValues().apply { put("file_name",file.name);put("size",file.length());put("modified",file.lastModified()) }
            db.insertWithOnConflict("documents",null,values,SQLiteDatabase.CONFLICT_IGNORE)
            val changed=ContentValues().apply {
                put("size",file.length());put("modified",file.lastModified());putNull("ocr_pages");putNull("ocr_at")
            }
            if(db.update("documents",changed,"file_name=? AND (size!=? OR modified!=?)",arrayOf(file.name,file.length().toString(),file.lastModified().toString()))>0)
                db.delete("ocr_pages","file_name=?",arrayOf(file.name))
        }
        val existing=files.map { it.name }.toSet();val entries=mutableMapOf<String,CatalogEntry>()
        val obsolete=mutableListOf<String>()
        db.rawQuery("SELECT file_name,folder_id,ocr_pages,ocr_at FROM documents",null).use { c ->
            while(c.moveToNext()) {
                val name=c.getString(0)
                if(name !in existing) obsolete.add(name)
                else entries[name]=CatalogEntry(if(c.isNull(1)) null else c.getString(1),if(c.isNull(2)) null else c.getInt(2),if(c.isNull(3)) null else c.getLong(3))
            }
        }
        obsolete.forEach { db.delete("documents","file_name=?",arrayOf(it)) }
        entries
    } }
    fun folders(): List<DocumentFolder> = database { db ->
        val result=mutableListOf<DocumentFolder>()
        db.rawQuery("SELECT id,name,color,icon FROM folders ORDER BY name_key,id",null).use { c -> while(c.moveToNext()) result.add(DocumentFolder(c.getString(0), c.getString(1),
            runCatching { FolderAppearance.color(c.getString(2)) }.getOrDefault(FolderAppearance.DEFAULT_COLOR),
            c.getString(3).takeIf { stored -> FolderIconKey.entries.any { it.id == stored } } ?: FolderAppearance.DEFAULT_ICON)) };result
    }
    fun createFolder(name: String, color: String = FolderAppearance.DEFAULT_COLOR,
                     icon: String = FolderAppearance.DEFAULT_ICON): DocumentFolder = database { db ->
        val cleaned=folderName(name)
        require(!folderExists(db,cleaned)) { "Nama folder sudah dipakai." }
        val folder=DocumentFolder(UUID.randomUUID().toString(), cleaned, FolderAppearance.color(color), FolderAppearance.icon(icon))
        db.insertOrThrow("folders",null,ContentValues().apply { put("id",folder.id);put("name",cleaned);put("name_key",cleaned.lowercase(Locale.ROOT));put("color",folder.color);put("icon",folder.icon) });folder
    }
    fun renameFolder(id: String,name: String) = database { db ->
        val cleaned=folderName(name)
        db.rawQuery("SELECT id FROM folders WHERE name_key=? AND id!=?",arrayOf(cleaned.lowercase(Locale.ROOT),id)).use { require(!it.moveToFirst()) { "Nama folder sudah dipakai." } }
        require(db.update("folders",ContentValues().apply { put("name",cleaned);put("name_key",cleaned.lowercase(Locale.ROOT)) },"id=?",arrayOf(id))==1) { "Folder tidak tersedia." }
    }
    fun updateFolder(id: String, name: String, color: String, icon: String) = database { db ->
        val cleaned = folderName(name)
        val normalizedColor = FolderAppearance.color(color)
        val normalizedIcon = FolderAppearance.icon(icon)
        db.rawQuery("SELECT id FROM folders WHERE name_key=? AND id!=?", arrayOf(cleaned.lowercase(Locale.ROOT), id)).use {
            require(!it.moveToFirst()) { "Nama folder sudah dipakai." }
        }
        require(db.update("folders", ContentValues().apply {
            put("name", cleaned); put("name_key", cleaned.lowercase(Locale.ROOT))
            put("color", normalizedColor); put("icon", normalizedIcon)
        }, "id=?", arrayOf(id)) == 1) { "Folder tidak tersedia." }
    }
    fun deleteFolder(id: String) = database { db -> db.delete("folders","id=?",arrayOf(id));Unit }
    fun move(fileName: String,folderId: String?) = database { db ->
        if(folderId!=null) require(db.rawQuery("SELECT id FROM folders WHERE id=?",arrayOf(folderId)).use { it.moveToFirst() }) { "Folder tidak tersedia." }
        require(db.update("documents",ContentValues().apply { if(folderId==null) putNull("folder_id") else put("folder_id",folderId) },"file_name=?",arrayOf(fileName))==1) { "Dokumen tidak tersedia." }
    }
    fun prepareRename(old: String,new: String) = database { db ->
        db.insertOrThrow("pending_rename",null,ContentValues().apply { put("old_name",old);put("new_name",new) });Unit
    }
    fun cancelRename(old: String) = database { db -> db.delete("pending_rename","old_name=?",arrayOf(old));Unit }
    fun finishRename(old: String,new: String) = database { db -> transaction(db) {
        db.execSQL("UPDATE documents SET file_name=? WHERE file_name=?",arrayOf(new,old))
        db.delete("pending_rename","old_name=?",arrayOf(old));Unit
    } }
    fun remove(name: String) = database { db -> db.delete("documents","file_name=?",arrayOf(name));Unit }
    fun saveOcr(file: File,pages: List<String>,expectedSize: Long,expectedModified: Long) = database { db -> transaction(db) {
        require(file.isFile && file.length()==expectedSize && file.lastModified()==expectedModified) { "Dokumen berubah. Jalankan OCR lagi." }
        val values=ContentValues().apply { put("ocr_pages",pages.size);put("ocr_at",System.currentTimeMillis()) }
        require(db.update("documents",values,"file_name=? AND size=? AND modified=?",arrayOf(file.name,expectedSize.toString(),expectedModified.toString()))==1) { "Dokumen berubah. Jalankan OCR lagi." }
        db.delete("ocr_pages","file_name=?",arrayOf(file.name))
        pages.forEachIndexed { index,text ->
            require(text.length<=200_000) { "Teks satu halaman terlalu panjang." }
            db.insertOrThrow("ocr_pages",null,ContentValues().apply {
                put("file_name",file.name);put("page_index",index);put("text",text);put("search_text",text.lowercase(Locale.ROOT))
            })
        }
    } }
    fun readOcr(name: String): OcrText? = database { db ->
        val timestamp=db.rawQuery("SELECT ocr_at FROM documents WHERE file_name=?",arrayOf(name)).use { c ->
            if(!c.moveToFirst() || c.isNull(0)) null else c.getLong(0)
        }
        if(timestamp==null) null else {
            val pages=mutableListOf<String>()
            db.rawQuery("SELECT text FROM ocr_pages WHERE file_name=? ORDER BY page_index",arrayOf(name)).use { c -> while(c.moveToNext()) pages.add(c.getString(0)) }
            OcrText(pages,timestamp)
        }
    }
    fun searchText(query: String): Set<String> = database { db ->
        val result=mutableSetOf<String>();val term=query.trim().lowercase(Locale.ROOT)
        if(term.isNotEmpty()) db.rawQuery("SELECT DISTINCT file_name FROM ocr_pages WHERE instr(search_text,?)>0",arrayOf(term)).use { c -> while(c.moveToNext()) result.add(c.getString(0)) }
        result
    }
    private fun folderExists(db: SQLiteDatabase,name: String)=db.rawQuery("SELECT id FROM folders WHERE name_key=?",arrayOf(name.lowercase(Locale.ROOT))).use { it.moveToFirst() }
    companion object {
        private val locks=ConcurrentHashMap<String,Any>()
        fun folderName(input: String): String {
            val name=input.trim()
            require(name.isNotBlank() && name.length<=60 && name.none { it.isISOControl() || it=='/' || it=='\\' }) { "Nama folder harus 1–60 karakter, tanpa garis miring atau karakter kontrol." }
            return name
        }
    }
}
