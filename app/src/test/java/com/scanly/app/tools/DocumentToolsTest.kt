package com.scanly.app.tools

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.zip.ZipInputStream
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import org.apache.pdfbox.cos.COSName
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DocumentToolsTest {
    private val app get()=ApplicationProvider.getApplicationContext<Application>()
    private fun work()=File(app.cacheDir,"tool-test-${UUID.randomUUID()}").apply { mkdirs() }
    private class Pages(override val count: Int=2): ToolPdfPages {
        var closed=false;val images=mutableListOf<Bitmap>()
        override fun render(index: Int,maxEdge: Int): RenderedPage {
            assertTrue(images.all { it.isRecycled })
            return RenderedPage(Bitmap.createBitmap(200,300,Bitmap.Config.ARGB_8888).apply { eraseColor(if(index%2==0) Color.WHITE else Color.BLUE);images.add(this) },200f,300f)
        }
        override fun close() { closed=true }
    }
    @Test fun imageImportPreservesExifRotationAndNormalizesToOwnedPng() {
        val directory=work();val source=File(directory,"source.jpg");val bitmap=Bitmap.createBitmap(80,140,Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE);DocumentTools.writeBitmap(bitmap,source,Bitmap.CompressFormat.JPEG);bitmap.recycle()
        androidx.exifinterface.media.ExifInterface(source).apply { setAttribute(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,"6");saveAttributes() }
        val bytes=source.readBytes();val output=File(directory,"normalized.png")
        DocumentTools.importImage(app,Uri.fromFile(source),output)
        val decoded=android.graphics.BitmapFactory.decodeFile(output.path)
        assertEquals(140,decoded.width);assertEquals(80,decoded.height);assertArrayEquals(bytes,source.readBytes());decoded.recycle()
        assertFalse(File(directory,"normalized.png.raw").exists())
    }
    @Test fun imageToPdfPreservesOrderAspectAndCleansIntermediateJpegs()=runBlocking {
        val directory=work();val images=listOf(Color.RED,Color.BLUE).mapIndexed { index,color ->
            val file=File(directory,"input-$index.png")
            val b=Bitmap.createBitmap(if(index==0) 200 else 100,if(index==0) 100 else 200,Bitmap.Config.ARGB_8888)
            b.eraseColor(color);DocumentTools.writeBitmap(b,file,Bitmap.CompressFormat.PNG);b.recycle();file
        }
        val output=File(directory,"result.pdf");DocumentTools.imagesToPdf(images,output,directory) { _,_ -> }
        Loader.loadPDF(output).use { pdf ->
            assertEquals(2,pdf.numberOfPages)
            assertEquals(2f,pdf.getPage(0).mediaBox.width/pdf.getPage(0).mediaBox.height,.001f)
            assertEquals(.5f,pdf.getPage(1).mediaBox.width/pdf.getPage(1).mediaBox.height,.001f)
            repeat(2) { index ->
                val image=pdf.getPage(index).resources.getXObject(COSName.getPDFName("Im0")) as PDImageXObject
                val rgb=image.createInputStream().use { it.readBytes() }
                assertTrue((rgb[if(index==0) 0 else 2].toInt() and 255)>230)
            }
        }
        assertTrue(images.all { it.isFile });assertFalse(directory.listFiles()!!.any { it.name.startsWith("render-") })
    }
    @Test fun singlePngAndAllJpgZipExportCorrectPageWithoutKeepingBitmaps()=runBlocking {
        val pages=Pages();val png=ByteArrayOutputStream()
        DocumentTools.exportPictures(File("source"),PictureFormat.PNG,1,png,{ _,_ -> },{ pages })
        val decoded=android.graphics.BitmapFactory.decodeByteArray(png.toByteArray(),0,png.size())
        assertEquals(Color.BLUE,decoded.getPixel(100,100));decoded.recycle();assertTrue(pages.closed)
        val batch=Pages(3);val output=ByteArrayOutputStream()
        DocumentTools.exportPictures(File("source"),PictureFormat.JPG,null,output,{ _,_ -> },{ batch })
        val names=mutableListOf<String>()
        ZipInputStream(output.toByteArray().inputStream()).use { zip ->
            while(true) { val entry=zip.nextEntry ?: break;names.add(entry.name);val bytes=zip.readBytes();assertEquals(255,bytes[0].toInt() and 255);assertEquals(216,bytes[1].toInt() and 255) }
        }
        assertEquals(listOf("halaman-1.jpg","halaman-2.jpg","halaman-3.jpg"),names)
        assertTrue(batch.closed);assertTrue(batch.images.all { it.isRecycled })
    }
    @Test fun cancellationClosesPdfAndStopsAfterCompletedPage()=runBlocking {
        val pages=Pages(4)
        try {
            DocumentTools.exportPictures(File("source"),PictureFormat.PNG,null,ByteArrayOutputStream(),{ done,_ -> if(done==1) throw CancellationException("Cancel") },{ pages })
            fail("Expected cancellation")
        } catch(_: CancellationException) {}
        assertTrue(pages.closed);assertEquals(1,pages.images.size);assertTrue(pages.images.single().isRecycled)
    }
    @Test fun signatureCopyChangesOnlyTargetPageAndRetainsPageSize()=runBlocking {
        val directory=work();val source=File(directory,"source.pdf").apply { writeText("original") };val original=source.readBytes()
        val ink=Bitmap.createBitmap(100,30,Bitmap.Config.ARGB_8888);ink.eraseColor(Color.BLACK)
        val signature=File(directory,"ink.png");DocumentTools.writeBitmap(ink,signature,Bitmap.CompressFormat.PNG);ink.recycle()
        val output=File(directory,"signed.pdf");val pages=Pages()
        DocumentTools.signPdf(source,signature,SignaturePosition(page=1,x=.25f,y=.5f,width=.5f),output,directory,{ _,_ -> },{ pages })
        Loader.loadPDF(output).use { pdf ->
            assertEquals(2,pdf.numberOfPages);assertEquals(200f,pdf.getPage(1).mediaBox.width,0f);assertEquals(300f,pdf.getPage(1).mediaBox.height,0f)
            val first=pdf.getPage(0).resources.getXObject(COSName.getPDFName("Im0")) as PDImageXObject
            val second=pdf.getPage(1).resources.getXObject(COSName.getPDFName("Im0")) as PDImageXObject
            val a=first.createInputStream().use { it.readBytes() };val b=second.createInputStream().use { it.readBytes() }
            val offset=(165*200+100)*3
            assertTrue((a[offset].toInt() and 255)>230)
            assertTrue((b[offset].toInt() and 255)<35 && (b[offset+2].toInt() and 255)<35)
        }
        assertArrayEquals(original,source.readBytes());assertTrue(pages.closed)
        assertFalse(directory.listFiles()!!.any { it.name.startsWith("signed-") })
    }
    @Test fun placementStaysInsidePageEvenForVeryTallSignature() {
        listOf(3f,.01f,10f).forEach { aspect ->
            val position=SignaturePosition(x=3f,y=-1f,width=.7f).bounded(.7f,aspect)
            assertTrue(position.x>=0 && position.y>=0)
            assertTrue(position.x+position.width<=1.001f)
            assertTrue(position.y+position.width*.7f/aspect<=1.001f)
        }
        assertThrows(IllegalArgumentException::class.java) { SignaturePosition(x=Float.NaN).bounded(1f,1f) }
    }
    @Test fun autoBackgroundRemovalKeepsBlueInkAndLeavesTransparentPaper() {
        val source=Bitmap.createBitmap(360,180,Bitmap.Config.ARGB_8888)
        for(y in 0 until 180) for(x in 0 until 360) { val tone=190+x*60/360;source.setPixel(x,y,Color.rgb(tone,tone,tone)) }
        Canvas(source).drawLine(70f,100f,280f,60f,Paint().apply { color=Color.BLUE;strokeWidth=8f;isAntiAlias=true })
        val before=source.getPixel(180,80);val clean=SignatureCleaner.clean(source)
        assertTrue(clean.width<source.width && clean.height<source.height)
        assertEquals(0,Color.alpha(clean.getPixel(0,0)))
        val pixels=IntArray(clean.width*clean.height);clean.getPixels(pixels,0,clean.width,0,0,clean.width,clean.height)
        assertTrue(pixels.any { Color.alpha(it)>200 && Color.blue(it)>200 && Color.red(it)<50 })
        assertEquals(before,source.getPixel(180,80));clean.recycle();source.recycle()
    }
    @Test fun transparentSignatureIsPreservedAndBlankPhotoIsRejected() {
        val signature=SignatureDrawing.bitmap(listOf(listOf(InkPoint(.2f,.2f),InkPoint(.8f,.7f))))
        val clean=SignatureCleaner.clean(signature)
        assertEquals(0,Color.alpha(clean.getPixel(0,0)));assertTrue(clean.width<signature.width)
        clean.recycle();signature.recycle()
        val empty=Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        assertThrows(IllegalArgumentException::class.java) { SignatureCleaner.clean(empty) };empty.recycle()
    }
}
