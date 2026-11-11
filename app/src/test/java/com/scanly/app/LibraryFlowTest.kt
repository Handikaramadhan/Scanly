package com.scanly.app

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.ViewModelProvider
import com.scanly.app.data.Document
import com.scanly.app.ocr.OcrPages
import com.scanly.app.ocr.PageRecognizer
import com.scanly.app.ocr.PdfOcrEngine
import com.scanly.app.ui.HomeViewModel
import com.scanly.app.ui.ScanlyApp
import com.scanly.app.ui.theme.ScanlyTheme
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],shadows=[PdfRendererTestShadow::class],qualifiers="w411dp-h891dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LibraryFlowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private val model get()=ViewModelProvider(compose.activity)[HomeViewModel::class.java]
    private var scannedFolder: String?=null
    private var exportedText: String?=null
    private fun idle() { compose.waitUntil(20_000) { !model.state.value.busy };compose.waitForIdle() }
    @Before fun reset() {
        idle()
        val app=model.getApplication<Application>()
        File(app.filesDir,"documents").listFiles().orEmpty().forEach { it.delete() }
        app.deleteDatabase("catalog.db")
        compose.runOnIdle { model.closeText();model.refresh() };idle()
        compose.activity.setContent { ScanlyTheme {
            ScanlyApp(model,onScan={ scannedFolder=it },onOpen={},onShare={},onExport={},onExportText={ exportedText=it.id })
        } }
        compose.waitForIdle()
    }
    private fun save(): Document {
        val file=File(compose.activity.cacheDir,"library-input.pdf")
        javaClass.getResourceAsStream("/sample.pdf")!!.use { input -> file.outputStream().use { input.copyTo(it) } }
        compose.runOnIdle { model.save(Uri.fromFile(file)) };idle()
        return model.state.value.documents.first()
    }
    private fun engine(text: String,started: CompletableDeferred<Unit>?=null,gate: CompletableDeferred<Unit>?=null)=PdfOcrEngine(
        { object: OcrPages {
            override val count=2
            override fun render(index: Int)=Bitmap.createBitmap(80,100,Bitmap.Config.ARGB_8888)
            override fun close() {}
        } }, { object: PageRecognizer {
            override suspend fun recognize(bitmap: Bitmap): String { started?.complete(Unit);gate?.await();return text }
            override fun close() {}
        } })
    private fun recognize(document: Document,text: String) {
        compose.runOnIdle { model.recognize(document,engine(text)) }
        idle();compose.waitUntil(15_000) { !model.state.value.loadingText && model.state.value.recognizedText!=null }
    }
    private fun screenshot(name: String,dialog: Boolean=false) {
        compose.runOnIdle {
            val view=if(dialog) org.robolectric.shadows.ShadowDialog.getLatestDialog().window!!.decorView else compose.activity.window.decorView
            val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            File("build/reports/screenshots/$name.png").apply { parentFile?.mkdirs() }.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
        }
    }
    @Test fun folderCreateMoveRenameDeleteAndScannerTargetKeepPdf() {
        val document=save();val bytes=document.file.readBytes()
        compose.onNodeWithText("Dokumen").performClick()
        // Dialog text-input idling is a Robolectric limitation also seen in PDF rename.
        // Validate the actual create/rename operations here; keyboard/form behavior stays in DEVICE_TESTS.
        compose.onNodeWithContentDescription("Buat folder").assertExists()
        compose.runOnIdle { model.createFolder("Arsip kerja") };idle()
        val folder=model.state.value.folders.single()
        compose.onNodeWithContentDescription("Opsi ${document.name}").performScrollTo().performClick()
        compose.onNodeWithText("Pindahkan ke folder").performClick()
        compose.onNodeWithContentDescription("Pilih folder Arsip kerja").performClick();idle()
        compose.onNodeWithContentDescription("Buat folder").performScrollTo()
        compose.onNode(hasText("Arsip kerja") and hasClickAction()).performClick()
        compose.onNodeWithText("Scan Dokumen",useUnmergedTree=true).performClick()
        assertEquals(folder.id,scannedFolder)
        screenshot("library-folders")
        compose.runOnIdle { model.renameFolder(folder.id,"Arsip kantor") };idle()
        assertEquals(folder.id,model.state.value.folders.single().id)
        compose.onNodeWithContentDescription("Opsi folder").performClick()
        compose.onNodeWithText("Hapus folder").performClick()
        compose.onNodeWithText("Hapus folder").performClick();idle()
        assertTrue(model.state.value.folders.isEmpty())
        assertNull(model.state.value.documents.single().folderId)
        assertArrayEquals(bytes,document.file.readBytes())
    }
    @Test fun ocrTextCanBeCopiedExportedAndFoundWithoutFilenameMatch() {
        val document=save()
        recognize(document,"Contoh OCR sintetis\nInvoice 24680")
        screenshot("library-ocr-text",true)
        compose.onNodeWithText("Salin semua").performClick()
        val clipboard=compose.activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assertTrue(clipboard.primaryClip!!.getItemAt(0).text.contains("Invoice 24680"))
        compose.onNodeWithText("Ekspor TXT").performClick();assertEquals(document.id,exportedText)
        compose.onNodeWithText("Tutup").performClick()
        compose.onNodeWithText("Dokumen").performClick()
        compose.onNode(hasSetTextAction()).performSemanticsAction(SemanticsActions.SetText) { it(AnnotatedString("INVOICE")) }
        compose.waitUntil(15_000) { !model.state.value.searching && document.id in model.state.value.textMatches }
        compose.onNodeWithText(document.name).performScrollTo().assertIsDisplayed()
        screenshot("library-ocr-search")
        assertFalse(document.name.contains("Invoice",ignoreCase=true))
    }
    @Test fun cancelledRepeatedOcrKeepsPreviousCompleteText() {
        val document=save();recognize(document,"Teks sebelumnya")
        compose.onNodeWithText("Tutup").performClick()
        val started=CompletableDeferred<Unit>();val gate=CompletableDeferred<Unit>()
        compose.runOnIdle {
            assertFalse("Unexpected busy state: ${model.state.value}",model.state.value.busy)
            model.recognize(model.state.value.documents.single(),engine("Teks baru",started,gate))
            assertNotNull(model.state.value.ocrProgress)
        }
        compose.waitUntil(15_000) { compose.onAllNodes(isRoot()).fetchSemanticsNodes();started.isCompleted }
        compose.onNodeWithText("Batalkan OCR").performScrollTo().performClick()
        compose.runOnIdle { gate.complete(Unit) };idle()
        compose.runOnIdle { model.showText(model.state.value.documents.single()) }
        compose.waitUntil(15_000) { !model.state.value.loadingText }
        assertEquals(listOf("Teks sebelumnya","Teks sebelumnya"),model.state.value.recognizedText!!.pages)
    }
}
