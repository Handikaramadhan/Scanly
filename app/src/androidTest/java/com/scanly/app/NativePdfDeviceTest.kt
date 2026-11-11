package com.scanly.app

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.scanly.app.data.Document
import com.scanly.app.data.DocumentRepository
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Runs only on a real device/emulator; never substitutes the Android PDF renderer. */
@RunWith(AndroidJUnit4::class)
class NativePdfDeviceTest {
    @Test fun nativePdfSaveRenameExportAndDelete() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val token = UUID.randomUUID().toString()
        val source = File(context.cacheDir, "$token-fixture.pdf")
        val exported = File(context.cacheDir, "$token-export.pdf")
        val repository = DocumentRepository(context)
        var owned: Document? = null
        try {
            instrumentation.context.assets.open("sample.pdf").use { input -> source.outputStream().use { input.copyTo(it) } }
            val saved = repository.save(Uri.fromFile(source))
            owned = Document(saved, 2)
            assertEquals(2, repository.list().single { it.id == saved.name }.pageCount)
            val renamedFile = repository.rename(owned, "Uji-$token")
            owned = Document(renamedFile, 2)
            DocumentRepository(context).export(renamedFile.name, Uri.fromFile(exported))
            assertArrayEquals(source.readBytes(), exported.readBytes())
            repository.delete(owned)
            assertFalse(renamedFile.exists())
            assertTrue(exported.exists())
        } finally {
            owned?.let { repository.delete(it) }
            source.delete()
            exported.delete()
        }
    }
    @Test fun bundledLatinOcrReadsTextFromNativePdfRenderer() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val file=File(context.cacheDir,"ocr-${UUID.randomUUID()}.pdf")
        try {
            val pdf=android.graphics.pdf.PdfDocument()
            try {
                val page=pdf.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(600,800,1).create())
                page.canvas.drawColor(android.graphics.Color.WHITE)
                val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color=android.graphics.Color.BLACK;textSize=36f;typeface=android.graphics.Typeface.DEFAULT_BOLD
                }
                page.canvas.drawText("SCANLY OCR TEST",40f,150f,paint)
                page.canvas.drawText("12345",40f,230f,paint)
                pdf.finishPage(page);file.outputStream().use { pdf.writeTo(it) }
            } finally { pdf.close() }
            val pages=com.scanly.app.ocr.PdfOcrEngine().recognize(file) { _,_ -> }
            assertEquals(1,pages.size)
            assertTrue(pages.single().contains("12345"))
            assertTrue(pages.single().contains("OCR",ignoreCase=true))
        } finally { file.delete() }
    }

    @Test fun nativeImagePdfPngAndSignatureRoundtripPreservesSource() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val directory=File(context.cacheDir,"tools-native-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            val input=File(directory,"input.png")
            val white=android.graphics.Bitmap.createBitmap(300,400,android.graphics.Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.WHITE) }
            com.scanly.app.tools.DocumentTools.writeBitmap(white,input,android.graphics.Bitmap.CompressFormat.PNG);white.recycle()
            val pdf=File(directory,"input.pdf")
            com.scanly.app.tools.DocumentTools.imagesToPdf(listOf(input),pdf,directory) { _,_ -> }
            val original=pdf.readBytes()
            val signature=File(directory,"signature.png")
            val ink=android.graphics.Bitmap.createBitmap(100,40,android.graphics.Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.BLACK) }
            com.scanly.app.tools.DocumentTools.writeBitmap(ink,signature,android.graphics.Bitmap.CompressFormat.PNG);ink.recycle()
            val signed=File(directory,"signed.pdf")
            com.scanly.app.tools.DocumentTools.signPdf(pdf,signature,com.scanly.app.tools.SignaturePosition(0,.2f,.3f,.4f),signed,directory,{ _,_ -> })
            val exported=java.io.ByteArrayOutputStream()
            com.scanly.app.tools.DocumentTools.exportPictures(signed,com.scanly.app.tools.PictureFormat.PNG,0,exported,{ _,_ -> })
            val bytes=exported.toByteArray();val image=android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.size)
            try { assertTrue(android.graphics.Color.red(image.getPixel((image.width*.3f).toInt(),(image.height*.35f).toInt()))<50) } finally { image.recycle() }
            assertArrayEquals(original,pdf.readBytes())
        } finally { directory.deleteRecursively() }
    }

}
