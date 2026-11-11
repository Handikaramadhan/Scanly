package com.scanly.app.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.util.UUID
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class DocumentCatalogTest {
    private val app get()=ApplicationProvider.getApplicationContext<Application>()
    private val directory get()=File(app.filesDir,"documents").apply { mkdirs() }
    @Before fun reset() { app.deleteDatabase("catalog.db");directory.listFiles().orEmpty().forEach { it.delete() } }
    private fun file()=File(directory,"${UUID.randomUUID()}.pdf").apply { writeText("%PDF-fixture") }
    @Test fun oldFilesStartUnfiledAndFoldersPersistWithoutMovingPdf() {
        val file=file();val bytes=file.readBytes();val catalog=DocumentCatalog(app)
        assertNull(catalog.synchronize(listOf(file))[file.name]!!.folderId)
        val folder=catalog.createFolder("  Surat Penting  ")
        catalog.move(file.name,folder.id)
        assertEquals("Surat Penting",DocumentCatalog(app).folders().single().name)
        assertEquals(folder.id,DocumentCatalog(app).synchronize(listOf(file))[file.name]!!.folderId)
        assertArrayEquals(bytes,file.readBytes());assertEquals(directory,file.parentFile)
    }
    @Test fun deletingFolderUnfilesPdfAndKeepsOcr() {
        val file=file();val catalog=DocumentCatalog(app);catalog.synchronize(listOf(file))
        val folder=catalog.createFolder("Arsip");catalog.move(file.name,folder.id)
        catalog.saveOcr(file,listOf("Nomor faktur 42",""),file.length(),file.lastModified())
        catalog.deleteFolder(folder.id)
        assertTrue(file.isFile);assertNull(catalog.synchronize(listOf(file))[file.name]!!.folderId)
        assertEquals(listOf("Nomor faktur 42",""),catalog.readOcr(file.name)!!.pages)
    }
    @Test fun duplicateAndInvalidFolderNamesAreRejected() {
        val catalog=DocumentCatalog(app);val folder=catalog.createFolder("Nota")
        assertThrows(IllegalArgumentException::class.java) { catalog.createFolder(" NOTA ") }
        listOf("","x/y","x\\y","x\ny","x".repeat(61)).forEach { name ->
            assertThrows(IllegalArgumentException::class.java) { catalog.createFolder(name) }
        }
        val second=catalog.createFolder("Surat")
        assertThrows(IllegalArgumentException::class.java) { catalog.renameFolder(second.id,"nota") }
        catalog.renameFolder(folder.id,"NOTA")
        assertEquals(2,catalog.folders().size)
    }
    @Test fun renameRecoveryFinishesMetadataAfterFileWasRenamed() {
        val file=file();val catalog=DocumentCatalog(app);catalog.synchronize(listOf(file))
        val folder=catalog.createFolder("Keuangan");catalog.move(file.name,folder.id)
        catalog.saveOcr(file,listOf("Pajak"),file.length(),file.lastModified())
        val renamed=File(directory,"Pajak.pdf")
        catalog.prepareRename(file.name,renamed.name)
        assertTrue(file.renameTo(renamed))
        val fresh=DocumentCatalog(app)
        // Synchronize itself must recover before pruning old filenames.
        assertEquals(folder.id,fresh.synchronize(listOf(renamed))[renamed.name]!!.folderId)
        assertEquals("Pajak",fresh.readOcr(renamed.name)!!.text)
        assertNull(fresh.readOcr(file.name))
    }
    @Test fun interruptedRenameBeforeFilesystemStepRetainsOriginalMetadata() {
        val file=file();val catalog=DocumentCatalog(app);catalog.synchronize(listOf(file))
        val folder=catalog.createFolder("Arsip");catalog.move(file.name,folder.id)
        catalog.prepareRename(file.name,"Belum.pdf")
        catalog.recover(directory)
        assertEquals(folder.id,catalog.synchronize(listOf(file))[file.name]!!.folderId)
        assertFalse(File(directory,"Belum.pdf").exists())
    }
    @Test fun changedPdfInvalidatesOcrButRetainsFolderAndRejectsStaleCommit() {
        val file=file();val catalog=DocumentCatalog(app);catalog.synchronize(listOf(file))
        val folder=catalog.createFolder("Arsip");catalog.move(file.name,folder.id)
        val length=file.length();val modified=file.lastModified()
        catalog.saveOcr(file,listOf("Teks lama"),length,modified)
        file.appendText(" changed")
        val entry=catalog.synchronize(listOf(file))[file.name]!!
        assertEquals(folder.id,entry.folderId);assertNull(entry.ocrPages);assertNull(catalog.readOcr(file.name))
        assertThrows(IllegalArgumentException::class.java) { catalog.saveOcr(file,listOf("stale"),length,modified) }
    }
    @Test fun ocrSearchIsLiteralCaseInsensitiveAndDoesNotMatchUnprocessedFiles() {
        val file=file();val other=file();val catalog=DocumentCatalog(app);catalog.synchronize(listOf(file,other))
        catalog.saveOcr(file,listOf("FAKTUR café 100%_paid"),file.length(),file.lastModified())
        assertEquals(setOf(file.name),catalog.searchText("Café"))
        assertEquals(setOf(file.name),catalog.searchText("%_"))
        assertTrue(catalog.searchText("% OR 1=1").isEmpty());assertTrue(catalog.searchText("").isEmpty())
        catalog.remove(file.name);assertTrue(catalog.searchText("faktur").isEmpty())
    }
    @Test fun longMultiPageTextLoadsInSeparateRowsAndFailedReplacementRollsBack() {
        val file=file();val catalog=DocumentCatalog(app);catalog.synchronize(listOf(file))
        val pages=List(20) { "Halaman $it invoice 100% " + "a".repeat(80_000) }
        catalog.saveOcr(file,pages,file.length(),file.lastModified())
        assertEquals(pages,catalog.readOcr(file.name)!!.pages)
        try { catalog.saveOcr(file,listOf("partial","x".repeat(200_001)),file.length(),file.lastModified());fail("Expected size guard") } catch(_: IllegalArgumentException) {}
        assertEquals(pages,catalog.readOcr(file.name)!!.pages)
    }

}
