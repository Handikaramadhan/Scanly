package com.scanly.app.tools

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.roundToInt
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

enum class ToolMode { IMAGES_TO_PDF, PDF_TO_IMAGES, SIGN_PDF }
enum class PictureFormat(val label: String,val extension: String,val mime: String,val compression: Bitmap.CompressFormat) {
    JPG("JPG","jpg","image/jpeg",Bitmap.CompressFormat.JPEG),PNG("PNG","png","image/png",Bitmap.CompressFormat.PNG)
}
data class SignaturePosition(val page: Int=0,val x: Float=.1f,val y: Float=.6f,val width: Float=.35f) {
    fun bounded(pageAspect: Float,signatureAspect: Float): SignaturePosition {
        require(listOf(pageAspect,signatureAspect,x,y,width).all { it.isFinite() } && pageAspect>0 && signatureAspect>0)
        val maximum=minOf(.85f,.85f*signatureAspect/pageAspect).coerceAtLeast(.001f)
        val w=width.coerceIn(minOf(.08f,maximum),maximum)
        val h=w*pageAspect/signatureAspect
        return copy(x=x.coerceIn(0f,1f-w),y=y.coerceIn(0f,1f-h),width=w)
    }
}
data class RenderedPage(val bitmap: Bitmap,val widthPoints: Float,val heightPoints: Float)

internal interface ToolPdfPages: java.io.Closeable { val count: Int;fun render(index: Int,maxEdge: Int=2200): RenderedPage }
internal class PdfPages(file: File): ToolPdfPages {
    private val descriptor=ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer=try { PdfRenderer(descriptor) } catch(error: Throwable) { descriptor.close();throw error }
    override val count get()=renderer.pageCount
    override fun render(index: Int,maxEdge: Int): RenderedPage=renderer.openPage(index).use { page ->
        val scale=minOf(3f,maxEdge.toFloat()/maxOf(page.width,page.height))
        val image=Bitmap.createBitmap((page.width*scale).roundToInt().coerceAtLeast(1),(page.height*scale).roundToInt().coerceAtLeast(1),Bitmap.Config.ARGB_8888)
        try { image.eraseColor(Color.WHITE);page.render(image,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);RenderedPage(image,page.width.toFloat(),page.height.toFloat()) }
        catch(error: Throwable) { image.recycle();throw error }
    }
    override fun close() { try { renderer.close() } finally { descriptor.close() } }
}

object DocumentTools {
    fun importImage(context: Context,uri: Uri,target: File,maxEdge: Int=2400) {
        val raw=File(target.parentFile,"${target.name}.raw")
        try {
            (context.contentResolver.openInputStream(uri) ?: throw IOException("Gambar tidak dapat dibaca.")).use { input ->
                raw.outputStream().use { output ->
                    val bytes=ByteArray(16384);var total=0L
                    while(true) { val count=input.read(bytes);if(count<0) break;total+=count;if(total>80_000_000) throw IOException("Gambar terlalu besar.");output.write(bytes,0,count) }
                }
            }
            val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true };BitmapFactory.decodeFile(raw.path,bounds)
            require(bounds.outWidth>0 && bounds.outHeight>0) { "Pilih berkas gambar yang dapat dibuka." }
            var sample=1;while(maxOf(bounds.outWidth,bounds.outHeight)/sample>maxEdge) sample*=2
            val source=BitmapFactory.decodeFile(raw.path,BitmapFactory.Options().apply { inSampleSize=sample;inPreferredConfig=Bitmap.Config.ARGB_8888 }) ?: throw IOException("Gambar tidak dapat dibuka.")
            var oriented: Bitmap?=null
            try {
                val matrix=Matrix()
                when(ExifInterface(raw).getAttributeInt(ExifInterface.TAG_ORIENTATION,1)) {
                    2 -> matrix.setScale(-1f,1f);3 -> matrix.setRotate(180f);4 -> matrix.setScale(1f,-1f)
                    5 -> { matrix.setRotate(90f);matrix.postScale(-1f,1f) };6 -> matrix.setRotate(90f)
                    7 -> { matrix.setRotate(90f);matrix.postScale(1f,-1f) };8 -> matrix.setRotate(270f)
                }
                oriented=Bitmap.createBitmap(source,0,0,source.width,source.height,matrix,true)
                writeBitmap(oriented,target,Bitmap.CompressFormat.PNG)
            } finally { if(oriented!==source) oriented?.recycle();source.recycle() }
        } finally { raw.delete() }
    }
    fun writeBitmap(bitmap: Bitmap,target: File,format: Bitmap.CompressFormat) = target.outputStream().use {
        if(!bitmap.compress(format,95,it)) throw IOException("Gambar gagal disimpan.")
        it.fd.sync()
    }
    suspend fun imagesToPdf(images: List<File>,output: File,work: File,progress: (Int,Int)->Unit) {
        require(images.isNotEmpty()) { "Pilih setidaknya satu gambar." }
        val pages=mutableListOf<ImagePdfWriter.JpegPage>()
        try {
            images.forEachIndexed { index,file ->
                currentCoroutineContext().ensureActive();progress(index,images.size)
                val source=BitmapFactory.decodeFile(file.path) ?: throw IOException("Gambar sesi tidak tersedia.")
                val white=Bitmap.createBitmap(source.width,source.height,Bitmap.Config.ARGB_8888)
                try {
                    Canvas(white).apply { drawColor(Color.WHITE);drawBitmap(source,0f,0f,null) }
                    val jpeg=File(work,"render-$index.jpg");writeBitmap(white,jpeg,Bitmap.CompressFormat.JPEG)
                    val scale=842f/maxOf(source.width,source.height)
                    pages.add(ImagePdfWriter.JpegPage(jpeg,source.width,source.height,source.width*scale,source.height*scale))
                } finally { source.recycle();white.recycle() }
            }
            val context=currentCoroutineContext();context.ensureActive();ImagePdfWriter.write(pages,output) { context.ensureActive() };progress(images.size,images.size)
        } finally { pages.forEach { it.file.delete() } }
    }
    internal suspend fun exportPictures(source: File,format: PictureFormat,page: Int?,output: OutputStream,progress: (Int,Int)->Unit,openPages: (File)->ToolPdfPages={ PdfPages(it) }) {
        openPages(source).use { pdf ->
            require(page==null || page in 0 until pdf.count)
            if(page!=null) {
                currentCoroutineContext().ensureActive();val image=pdf.render(page).bitmap
                try { if(!image.compress(format.compression,95,output)) throw IOException("Ekspor gambar gagal.") } finally { image.recycle() }
                progress(1,1)
            } else ZipOutputStream(output).use { zip ->
                repeat(pdf.count) { index ->
                    currentCoroutineContext().ensureActive();progress(index,pdf.count)
                    val image=pdf.render(index).bitmap
                    try { zip.putNextEntry(ZipEntry("halaman-${index+1}.${format.extension}"));if(!image.compress(format.compression,95,zip)) throw IOException("Ekspor gambar gagal.");zip.closeEntry() } finally { image.recycle() }
                };progress(pdf.count,pdf.count)
            }
        }
    }
    internal suspend fun signPdf(source: File,signature: File,position: SignaturePosition,output: File,work: File,progress: (Int,Int)->Unit,openPages: (File)->ToolPdfPages={ PdfPages(it) }) {
        val ink=BitmapFactory.decodeFile(signature.path) ?: throw IOException("Tanda tangan belum tersedia.")
        val rendered=mutableListOf<ImagePdfWriter.JpegPage>()
        try { openPages(source).use { pdf ->
            require(position.page in 0 until pdf.count)
            repeat(pdf.count) { index ->
                currentCoroutineContext().ensureActive();progress(index,pdf.count)
                val page=pdf.render(index);val image=page.bitmap
                try {
                    if(index==position.page) {
                        val p=position.bounded(image.width.toFloat()/image.height,ink.width.toFloat()/ink.height)
                        val w=p.width*image.width;val h=w*ink.height/ink.width
                        Canvas(image).drawBitmap(ink,null,RectF(p.x*image.width,p.y*image.height,p.x*image.width+w,p.y*image.height+h),Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                    }
                    val jpeg=File(work,"signed-$index.jpg");writeBitmap(image,jpeg,Bitmap.CompressFormat.JPEG)
                    rendered.add(ImagePdfWriter.JpegPage(jpeg,image.width,image.height,page.widthPoints,page.heightPoints))
                } finally { image.recycle() }
            }
            val context=currentCoroutineContext();context.ensureActive();ImagePdfWriter.write(rendered,output) { context.ensureActive() };progress(pdf.count,pdf.count)
        } } finally { ink.recycle();rendered.forEach { it.file.delete() } }
    }
}
