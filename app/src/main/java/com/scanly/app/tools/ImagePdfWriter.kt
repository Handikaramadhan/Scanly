package com.scanly.app.tools

import java.io.File
import java.io.OutputStream
import java.util.Locale

/** Image-only PDF 1.4 writer: JPEGs stream to disk without retaining every page bitmap in memory. */
object ImagePdfWriter {
    data class JpegPage(val file: File, val width: Int, val height: Int, val pageWidth: Float=595f, val pageHeight: Float=842f)
    fun write(pages: List<JpegPage>, target: File,checkCancellation: ()->Unit = {}) {
        require(pages.isNotEmpty()) { "Tambahkan setidaknya satu halaman." }
        require(pages.all { it.width > 0 && it.height > 0 && it.file.isFile })
        target.outputStream().use { raw ->
            val output = CountingOutput(raw)
            fun text(value: String) = output.write(value.toByteArray(Charsets.US_ASCII))
            val offsets = mutableListOf(0L)
            fun obj(number: Int, block: () -> Unit) {
                check(number == offsets.size)
                offsets.add(output.position)
                text("$number 0 obj\n"); block(); text("\nendobj\n")
            }
            text("%PDF-1.4\n")
            obj(1) { text("<< /Type /Catalog /Pages 2 0 R >>") }
            obj(2) { text("<< /Type /Pages /Count ${pages.size} /Kids [${pages.indices.joinToString(" ") { "${3 + it * 3} 0 R" }}] >>") }
            pages.forEachIndexed { index, page ->
                checkCancellation()
                val number = 3 + index * 3
                val pageWidth=page.pageWidth;val pageHeight=page.pageHeight
                require(pageWidth.isFinite() && pageHeight.isFinite() && pageWidth>0 && pageHeight>0)
                val command=String.format(Locale.ROOT,"q %.3f 0 0 %.3f 0 0 cm /Im0 Do Q\n",pageWidth,pageHeight)
                val media=String.format(Locale.ROOT,"0 0 %.3f %.3f",pageWidth,pageHeight)
                obj(number) { text("<< /Type /Page /Parent 2 0 R /MediaBox [$media] /Resources << /XObject << /Im0 ${number+1} 0 R >> >> /Contents ${number+2} 0 R >>") }
                obj(number+1) {
                    text("<< /Type /XObject /Subtype /Image /Width ${page.width} /Height ${page.height} /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${page.file.length()} >>\nstream\n")
                    page.file.inputStream().use { it.copyTo(output) }; text("\nendstream")
                }
                obj(number+2) { text("<< /Length ${command.toByteArray(Charsets.US_ASCII).size} >>\nstream\n${command}endstream") }
            }
            val crossReference = output.position
            text("xref\n0 ${offsets.size}\n0000000000 65535 f \n")
            offsets.drop(1).forEach { offset ->
                require(offset <= 9_999_999_999L) { "PDF terlalu besar untuk format ini." }
                text(String.format(Locale.ROOT, "%010d 00000 n \n", offset))
            }
            text("trailer\n<< /Size ${offsets.size} /Root 1 0 R >>\nstartxref\n$crossReference\n%%EOF\n")
            output.flush(); raw.fd.sync()
        }
    }
    private class CountingOutput(private val delegate: OutputStream) : OutputStream() {
        var position = 0L; private set
        override fun write(value: Int) { delegate.write(value); position++ }
        override fun write(bytes: ByteArray, offset: Int, length: Int) { delegate.write(bytes,offset,length); position += length }
        override fun flush() = delegate.flush()
    }
}
