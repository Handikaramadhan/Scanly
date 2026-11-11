package com.scanly.app.ocr

import android.graphics.Bitmap
import java.io.File
import java.io.IOException
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class PdfOcrEngineTest {
    private class Pages(override val count: Int=3): OcrPages {
        var closed=false;val images=mutableListOf<Bitmap>()
        override fun render(index: Int)=Bitmap.createBitmap(40,60,Bitmap.Config.ARGB_8888).also { images.add(it) }
        override fun close() { closed=true }
    }
    @Test fun pagesAreRecognizedInOrderAndBitmapsRecycledBeforeNextPage() = runBlocking {
        val pages=Pages();var closed=false;var calls=0
        val recognizer=object: PageRecognizer {
            override suspend fun recognize(bitmap: Bitmap): String {
                assertFalse(bitmap.isRecycled)
                assertTrue(pages.images.dropLast(1).all { it.isRecycled })
                return "Halaman ${++calls}"
            }
            override fun close() { closed=true }
        }
        val progress=mutableListOf<Pair<Int,Int>>()
        val result=PdfOcrEngine({ pages },{ recognizer }).recognize(File("fixture")) { p,n -> progress.add(p to n) }
        assertEquals(listOf("Halaman 1","Halaman 2","Halaman 3"),result)
        assertEquals(3 to 3,progress.last());assertTrue(closed);assertTrue(pages.closed);assertTrue(pages.images.all { it.isRecycled })
    }
    @Test fun cancellationWaitsForNativeTaskBeforeRecyclingAndStopsBeforeNextPage() = runBlocking {
        val pages=Pages();val started=CompletableDeferred<Unit>();val complete=CompletableDeferred<Unit>();var returned=false;var closed=false
        val recognizer=object: PageRecognizer {
            override suspend fun recognize(bitmap: Bitmap): String { started.complete(Unit);complete.await();assertFalse(bitmap.isRecycled);return "partial" }
            override fun close() { closed=true }
        }
        val job=launch { PdfOcrEngine({ pages },{ recognizer }).recognize(File("fixture")) { _,_ -> };returned=true }
        started.await();job.cancel()
        assertFalse(pages.images.single().isRecycled)
        complete.complete(Unit);job.join()
        assertFalse(returned);assertTrue(closed);assertTrue(pages.closed);assertEquals(1,pages.images.size);assertTrue(pages.images.single().isRecycled)
    }
    @Test fun failureClosesRecognizerPagesAndBitmapWithoutPartialResult() = runBlocking {
        val pages=Pages();var closed=false
        val recognizer=object: PageRecognizer {
            override suspend fun recognize(bitmap: Bitmap): String { throw IOException("Recognition failed") }
            override fun close() { closed=true }
        }
        try { PdfOcrEngine({ pages },{ recognizer }).recognize(File("fixture")) { _,_ -> };fail("Expected failure") } catch(_: IOException) {}
        assertTrue(pages.closed);assertTrue(closed);assertEquals(1,pages.images.size);assertTrue(pages.images.single().isRecycled)
    }
    @Test fun emptyAndOversizedResultsHaveDistinctBehavior() = runBlocking {
        fun recognizer(text: String)=object: PageRecognizer {
            override suspend fun recognize(bitmap: Bitmap)=text
            override fun close() {}
        }
        assertEquals(listOf(""),PdfOcrEngine({ Pages(1) },{ recognizer("") }).recognize(File("fixture")) { _,_ -> })
        val pages=Pages(1)
        try { PdfOcrEngine({ pages },{ recognizer("x".repeat(2_000_001)) }).recognize(File("fixture")) { _,_ -> };fail("Expected size limit") } catch(_: IOException) {}
        assertTrue(pages.closed);assertTrue(pages.images.single().isRecycled)
    }
}
