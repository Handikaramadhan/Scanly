package com.scanly.app.ocr

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal interface OcrPages: Closeable {
    val count: Int
    /** Returns an owned bitmap; the engine recycles it after recognition completes. */
    fun render(index: Int): Bitmap
}
internal interface PageRecognizer: Closeable { suspend fun recognize(bitmap: Bitmap): String }

internal class AndroidPdfPages(file: File): OcrPages {
    private val descriptor=ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer=try { PdfRenderer(descriptor) } catch(error: Throwable) { descriptor.close();throw error }
    override val count get()=renderer.pageCount
    override fun render(index: Int): Bitmap = renderer.openPage(index).use { page ->
        val scale=minOf(2.5f,2000f/maxOf(page.width,page.height).coerceAtLeast(1))
        val width=(page.width*scale).roundToInt().coerceAtLeast(1)
        val height=(page.height*scale).roundToInt().coerceAtLeast(1)
        val bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        } catch(error: Throwable) { bitmap.recycle();throw error }
    }
    override fun close() { try { renderer.close() } finally { descriptor.close() } }
}
private class LatinPageRecognizer: PageRecognizer {
    private val recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    override suspend fun recognize(bitmap: Bitmap): String = suspendCoroutine { continuation ->
        try {
            recognizer.process(InputImage.fromBitmap(bitmap,0)).addOnCompleteListener(Executor { it.run() }) { task ->
                if(task.isSuccessful) continuation.resume(task.result.text)
                else continuation.resumeWithException(task.exception ?: IOException("OCR belum berhasil."))
            }
        } catch(error: Exception) { continuation.resumeWithException(error) }
    }
    override fun close() { recognizer.close() }
}

class PdfOcrEngine internal constructor(
    private val openPages: (File)->OcrPages = { AndroidPdfPages(it) },
    private val openRecognizer: ()->PageRecognizer = { LatinPageRecognizer() }
) {
    suspend fun recognize(file: File,onProgress: (Int,Int)->Unit): List<String> = withContext(Dispatchers.IO) {
        openPages(file).use { pages ->
            require(pages.count>0) { "PDF tidak memiliki halaman." }
            openRecognizer().use { recognizer ->
                val results=ArrayList<String>();var characters=0
                for(index in 0 until pages.count) {
                    currentCoroutineContext().ensureActive()
                    onProgress(index,pages.count)
                    val bitmap=pages.render(index)
                    val text=try {
                        // Wait for the native task before recycling a bitmap, including cancellation.
                        withContext(NonCancellable) { recognizer.recognize(bitmap) }
                    } finally { bitmap.recycle() }
                    currentCoroutineContext().ensureActive()
                    if(text.length>200_000) throw IOException("Teks satu halaman terlalu panjang.")
                    characters+=text.length
                    if(characters>2_000_000) throw IOException("Hasil OCR terlalu besar. Proses PDF yang lebih pendek.")
                    results.add(text)
                    onProgress(index+1,pages.count)
                }
                results
            }
        }
    }
}
