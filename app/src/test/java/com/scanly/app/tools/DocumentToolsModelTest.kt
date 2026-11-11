package com.scanly.app.tools

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Looper
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.scanly.app.PdfRendererTestShadow
import java.io.File
import java.util.UUID
import org.apache.pdfbox.Loader
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],shadows=[PdfRendererTestShadow::class])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DocumentToolsModelTest {
    private val app get()=ApplicationProvider.getApplicationContext<Application>()
    private fun await(model: DocumentToolsModel,allowMessage: Boolean=false) {
        val deadline=System.currentTimeMillis()+15000
        while(model.state.value.busy || model.state.value.loading) { shadowOf(Looper.getMainLooper()).idle();Thread.sleep(10);if(System.currentTimeMillis()>deadline) fail("Tools timed out") }
        shadowOf(Looper.getMainLooper()).idle()
        if(!allowMessage) assertNull(model.state.value.message)
    }
    private fun image(color: Int): File {
        val file=File(app.cacheDir,"input-${UUID.randomUUID()}.png")
        val b=Bitmap.createBitmap(80,120,Bitmap.Config.ARGB_8888).apply { eraseColor(color) }
        DocumentTools.writeBitmap(b,file,Bitmap.CompressFormat.PNG);b.recycle();return file
    }
    @Test fun imageOrderPersistsAndCompletedSessionIsIdempotent() {
        val handle=SavedStateHandle();val model=DocumentToolsModel(app,handle)
        model.initialize(ToolMode.IMAGES_TO_PDF,null,null);await(model)
        val inputs=listOf(image(Color.RED),image(Color.GREEN),image(Color.BLUE))
        model.importImages(inputs.map(Uri::fromFile));await(model)
        val blue=model.state.value.images[2]
        model.moveImage(2,-1);model.removeImage(0)
        assertEquals(blue,model.state.value.images.first())
        val restored=DocumentToolsModel(app,handle);restored.initialize(ToolMode.IMAGES_TO_PDF,null,null);await(restored)
        assertEquals(model.state.value.images,restored.state.value.images)
        restored.createPdf();await(restored)
        val id=restored.state.value.completedId!!;val output=File(app.filesDir,"documents/$id")
        Loader.loadPDF(output).use { assertEquals(2,it.numberOfPages) }
        val again=DocumentToolsModel(app,handle);again.initialize(ToolMode.IMAGES_TO_PDF,null,null);await(again)
        assertEquals(id,again.state.value.completedId);assertTrue(inputs.all { it.isFile })
    }
    @Test fun blankSignatureCannotBecomeAnActiveStamp() {
        val model=DocumentToolsModel(app,SavedStateHandle());model.initialize(ToolMode.IMAGES_TO_PDF,null,null);await(model)
        val ink=SignatureDrawing.bitmap(listOf(listOf(InkPoint(.2f,.3f),InkPoint(.8f,.7f))))
        model.drawnSignature(ink);await(model);assertNotNull(model.state.value.signature)
        model.importSignature(Uri.fromFile(image(Color.WHITE)));await(model,true)
        assertNull(model.state.value.signature);assertNotNull(model.state.value.message)
    }
    @Test fun galleryResultDuringInitializationIsQueuedInsteadOfLost() {
        val model=DocumentToolsModel(app,SavedStateHandle())
        val source=image(Color.RED)
        model.importImages(listOf(Uri.fromFile(source)))
        model.initialize(ToolMode.IMAGES_TO_PDF,null,null);await(model)
        assertEquals(1,model.state.value.images.size);assertTrue(model.state.value.images.single().isFile)
    }

}
