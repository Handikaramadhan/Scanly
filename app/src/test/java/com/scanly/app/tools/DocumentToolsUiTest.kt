package com.scanly.app.tools

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.scanly.app.MainActivity
import com.scanly.app.ui.theme.ScanlyTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w411dp-h891dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DocumentToolsUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun screenshot(name: String,dialog: Boolean=false) { compose.runOnIdle {
        val view=if(dialog) org.robolectric.shadows.ShadowDialog.getLatestDialog().window!!.decorView else compose.activity.window.decorView
        val b=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888);view.draw(android.graphics.Canvas(b))
        File("build/reports/screenshots/$name.png").apply { parentFile?.mkdirs() }.outputStream().use { b.compress(Bitmap.CompressFormat.PNG,100,it) };b.recycle()
    } }
    @Test fun drawUndoClearAndApplyProduceTransparentSignature() {
        var result: Bitmap?=null
        compose.waitForIdle()
        compose.activity.setContent { ScanlyTheme { Box(Modifier.fillMaxSize()) { DrawSignatureDialog({}, { result=it }) } } }
        compose.onNodeWithText("Gunakan TTD").assertIsNotEnabled()
        fun stroke() { compose.onNodeWithTag("signature-pad").performTouchInput { swipe(Offset(width*.2f,height*.7f),Offset(width*.8f,height*.3f),400) } }
        stroke();compose.onNodeWithText("Gunakan TTD").assertIsEnabled()
        screenshot("tools-signature-draw",true)
        compose.onNodeWithText("Urungkan").performClick();compose.onNodeWithText("Gunakan TTD").assertIsNotEnabled()
        stroke();compose.onNodeWithText("Bersihkan").performClick();compose.onNodeWithText("Gunakan TTD").assertIsNotEnabled()
        stroke();compose.onNodeWithText("Gunakan TTD").performClick()
        assertNotNull(result);assertEquals(0,android.graphics.Color.alpha(result!!.getPixel(0,0)));result!!.recycle()
    }
    @Test fun previewPlacementMatchesFitAreaAndClampsToPage() {
        val page=Bitmap.createBitmap(600,800,Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.WHITE) }
        android.graphics.Canvas(page).drawText("Contoh PDF sintetis",40f,80f,android.graphics.Paint().apply { textSize=36f;color=android.graphics.Color.DKGRAY })
        val ink=SignatureDrawing.bitmap(listOf(listOf(InkPoint(.1f,.6f),InkPoint(.4f,.2f),InkPoint(.6f,.7f),InkPoint(.9f,.4f))))
        var position by mutableStateOf(SignaturePosition())
        compose.activity.setContent { ScanlyTheme { PdfToolPreview(page,ink,position,true,{ position=it },Modifier.fillMaxSize()) } }
        compose.onNodeWithTag("tool-page-preview").performTouchInput { click(Offset(width*.9f,height*.8f)) }
        assertTrue(position.x>0.4f)
        assertTrue(position.x+position.width<=1.001f)
        assertTrue(position.y+position.width*.75f/(ink.width.toFloat()/ink.height)<=1.001f)
        screenshot("tools-signature-position")
        page.recycle();ink.recycle()
    }
}
