package com.scanly.app.data

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [com.scanly.app.PdfRendererTestShadow::class])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DocumentRepositoryTest {
    @Test fun realPdfRoundtripExportAndReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val source = File(context.cacheDir, "fixture.pdf")
        javaClass.getResourceAsStream("/sample.pdf")!!.use { input -> source.outputStream().use { input.copyTo(it) } }
        val repository = DocumentRepository(context)
        val saved = repository.save(Uri.fromFile(source))
        assertArrayEquals(source.readBytes(), saved.readBytes())
        val document = repository.list().single()
        assertEquals(2, document.pageCount)
        val target = File(context.cacheDir, "export.pdf")
        DocumentRepository(context).export(document.id, Uri.fromFile(target))
        assertArrayEquals(source.readBytes(), target.readBytes())
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.export(document.id, Uri.fromFile(saved)) }
        }
        assertArrayEquals(source.readBytes(), saved.readBytes())
        repository.delete(document)
        assertTrue(repository.list().isEmpty())
        assertTrue(target.exists())
    }
    private fun source(context: Application): File = File(context.cacheDir,"input-${java.util.UUID.randomUUID()}.pdf").also { file ->
        javaClass.getResourceAsStream("/sample.pdf")!!.use { input -> file.outputStream().use { input.copyTo(it) } }
    }
    private fun fakeOcr(text: List<String>,failAt: Int=-1)=com.scanly.app.ocr.PdfOcrEngine(
        { object: com.scanly.app.ocr.OcrPages {
            override val count=text.size
            override fun render(index: Int)=android.graphics.Bitmap.createBitmap(40,60,android.graphics.Bitmap.Config.ARGB_8888)
            override fun close() {}
        } }, { object: com.scanly.app.ocr.PageRecognizer {
            var index=0
            override suspend fun recognize(bitmap: android.graphics.Bitmap): String {
                if(index==failAt) throw java.io.IOException("Failed OCR")
                return text[index++]
            }
            override fun close() {}
        } })
    @Test fun folderOcrRenameAndTextExportKeepOriginalPdfIntact() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<Application>()
        val repository=DocumentRepository(context);val input=source(context)
        val folder=repository.createFolder("Folder-${java.util.UUID.randomUUID()}")
        val saved=repository.save(Uri.fromFile(input),folder.id)
        repository.recognize(saved.name,fakeOcr(listOf("Faktur café 123","Halaman kedua"))) { _,_ -> }
        val before=repository.list().single { it.id==saved.name }
        assertEquals(folder.id,before.folderId);assertEquals(2,before.ocrPages)
        val renamed=repository.rename(before,"Baru-${java.util.UUID.randomUUID()}")
        val fresh=DocumentRepository(context)
        val document=fresh.list().single { it.id==renamed.name }
        assertEquals(folder.id,document.folderId)
        assertEquals(setOf(renamed.name),fresh.searchText("FAKTUR"))
        val exported=File(context.cacheDir,"ocr-${java.util.UUID.randomUUID()}.txt")
        fresh.exportText(document.id,Uri.fromFile(exported))
        assertEquals("Faktur café 123\n\nHalaman kedua",exported.readText())
        assertArrayEquals(input.readBytes(),renamed.readBytes())
        fresh.deleteFolder(folder.id)
        assertNull(fresh.list().single { it.id==document.id }.folderId)
        assertNotNull(fresh.readOcr(document.id));assertTrue(renamed.isFile)
        fresh.delete(document);assertTrue(fresh.searchText("Faktur").isEmpty())
    }
    @Test fun failedRepeatedOcrKeepsLastCompleteResultAndRejectsExportOverPdf() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<Application>()
        val repository=DocumentRepository(context);val saved=repository.save(Uri.fromFile(source(context)))
        repository.recognize(saved.name,fakeOcr(listOf("old one","old two"))) { _,_ -> }
        try { repository.recognize(saved.name,fakeOcr(listOf("new one","new two"),failAt=1)) { _,_ -> };fail("Expected failure") } catch(_: java.io.IOException) {}
        assertEquals(listOf("old one","old two"),repository.readOcr(saved.name)!!.pages)
        val bytes=saved.readBytes()
        try { repository.exportText(saved.name,Uri.fromFile(saved));fail("Expected source guard") } catch(_: IllegalArgumentException) {}
        assertArrayEquals(bytes,saved.readBytes())
        repository.delete(repository.list().single { it.id==saved.name })
    }
    @Test fun failedRenameCollisionRetainsFolderAndOcrMetadata() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<Application>();val repository=DocumentRepository(context)
        val first=repository.save(Uri.fromFile(source(context)));val second=repository.save(Uri.fromFile(source(context)))
        val folder=repository.createFolder("Collision-${java.util.UUID.randomUUID()}")
        repository.move(repository.list().single { it.id==first.name },folder.id)
        repository.recognize(first.name,fakeOcr(listOf("unchanged"))) { _,_ -> }
        try { repository.rename(repository.list().single { it.id==first.name },second.nameWithoutExtension);fail("Expected collision") } catch(_: IllegalArgumentException) {}
        assertEquals(folder.id,repository.list().single { it.id==first.name }.folderId)
        assertEquals("unchanged",repository.readOcr(first.name)!!.text)
        assertTrue(first.isFile);assertTrue(second.isFile)
        repository.delete(repository.list().single { it.id==first.name });repository.delete(repository.list().single { it.id==second.name });repository.deleteFolder(folder.id)
    }

}
