package com.scanly.app

import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import org.apache.pdfbox.Loader
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

/** Robolectric has no native PDF engine. PDFBox is test-only; device tests use Android PdfRenderer. */
@Implements(PdfRenderer::class)
class PdfRendererTestShadow {
    private var descriptor: ParcelFileDescriptor? = null
    private var pages = 0
    @Implementation
    fun __constructor__(input: ParcelFileDescriptor) {
        descriptor = input
        val bytes = FileInputStream(input.fileDescriptor).readBytes()
        pages = Loader.loadPDF(bytes).use { it.numberOfPages }
    }
    @Implementation fun getPageCount(): Int = pages
    @Implementation fun close() { descriptor?.close(); descriptor = null }
}
