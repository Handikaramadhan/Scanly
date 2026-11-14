package com.scanly.app

import androidx.activity.compose.setContent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import com.scanly.app.data.DocumentRepository
import com.scanly.app.data.AppPreferences
import com.scanly.app.data.ThemeMode
import com.scanly.app.ui.HomeViewModel
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [com.scanly.app.PdfRendererTestShadow::class], qualifiers = "w411dp-h891dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScanlyFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Before fun resetTestFixtures() {
        // Only the simulator's isolated app directory; never a connected user's data.
        awaitIdle()
        val app = model().getApplication<android.app.Application>()
        File(app.filesDir, "documents").listFiles().orEmpty().forEach { it.delete() }
        app.deleteDatabase("catalog.db")
        compose.runOnIdle { model().closeText(); model().setTheme(ThemeMode.SYSTEM); model().refresh() }
        awaitIdle()
    }
    private fun model() = ViewModelProvider(compose.activity)[HomeViewModel::class.java]
    private fun awaitIdle() {
        compose.waitUntil(20_000) { !model().state.value.busy }
        compose.waitForIdle()
    }
    private fun fixture(name: String = "fixture.pdf"): File = File(model().getApplication<android.app.Application>().cacheDir, name).also { file ->
        javaClass.getResourceAsStream("/sample.pdf")!!.use { input -> file.outputStream().use { input.copyTo(it) } }
    }
    private fun screenshot(name: String) {
        val file = File("build/reports/screenshots/$name.png").apply { parentFile?.mkdirs() }
        lateinit var bitmap: Bitmap
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
        }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    @Test fun navigationThemeAndPrivacySurviveRecreation() {
        awaitIdle()
        compose.onNodeWithText("Mulai dari satu halaman").assertIsDisplayed()
        screenshot("home-light")
        compose.onNodeWithText("Pengaturan").performClick()
        compose.onNodeWithText("Gelap").performClick()
        compose.runOnIdle { assertEquals(ThemeMode.DARK, AppPreferences(model().getApplication()).theme()) }
        screenshot("settings-dark")
        compose.activityRule.scenario.recreate()
        awaitIdle()
        compose.onNodeWithText("Gelap").assertIsDisplayed()
        compose.onNodeWithText("Baca kebijakan privasi").performScrollTo().performClick()
        compose.onNodeWithText("Kebijakan privasi").assertIsDisplayed()
        compose.onNodeWithText("Tutup").performClick()
    }

    @Test fun queuedSaveRenameSearchExportAndDeleteKeepPdfIntact() {
        awaitIdle()
        val source = fixture()
        compose.runOnIdle { model().refresh(); model().save(Uri.fromFile(source)) }
        awaitIdle()
        assertEquals("Queued save should create a document: ${model().state.value}", 1, model().state.value.documents.size)
        val document = model().state.value.documents.single()
        assertEquals(2, document.pageCount)
        // Drive the data operation directly: Robolectric's dialog text-input idling is not a device UI test.
        // The rename form/focus/keyboard path remains in docs/DEVICE_TESTS.md.
        compose.runOnIdle { model().rename(document, "Nota café") }
        compose.waitUntil(20_000) { model().state.value.documents.singleOrNull()?.name == "Nota café" && !model().state.value.busy }
        val renamed = model().state.value.documents.single()
        assertArrayEquals(source.readBytes(), renamed.file.readBytes())
        compose.onNodeWithText("Dokumen").performClick()
        compose.onNode(hasSetTextAction()).performSemanticsAction(SemanticsActions.SetText) { it(AnnotatedString("NOTA")) }
        compose.onNodeWithText("Nota café").assertIsDisplayed()
        screenshot("documents-search")
        compose.onNode(hasSetTextAction()).performSemanticsAction(SemanticsActions.SetText) { it(AnnotatedString("tidak-ada")) }
        compose.waitUntil(15_000) { !model().state.value.searching }
        compose.onNodeWithText("Dokumen tidak ditemukan").assertIsDisplayed()
        compose.onNodeWithContentDescription("Hapus pencarian").performClick()
        val exported = File(model().getApplication<android.app.Application>().cacheDir, "exported.pdf")
        compose.runOnIdle { model().refresh(); model().export(renamed.id, Uri.fromFile(exported)) }
        awaitIdle()
        assertArrayEquals(source.readBytes(), exported.readBytes())
        val reloaded = runBlocking { DocumentRepository(model().getApplication()).list() }
        assertEquals("Nota café", reloaded.single().name)
        assertEquals(2, reloaded.single().pageCount)
        compose.onNodeWithContentDescription("Opsi Nota café").performClick()
        compose.onNodeWithText("Hapus").performClick()
        compose.onNodeWithText("Batal").performClick()
        assertTrue(renamed.file.exists())
        compose.onNodeWithContentDescription("Opsi Nota café").performClick()
        compose.onNodeWithText("Hapus").performClick()
        compose.onNodeWithText("Hapus", substring = false).performClick()
        compose.waitUntil(20_000) { model().state.value.documents.isEmpty() && !model().state.value.busy }
        assertFalse(renamed.file.exists())
        assertTrue(exported.exists())
    }

    @Test fun malformedPdfIsRejectedWithoutCommittedOrPartialFiles() {
        awaitIdle()
        val invalid = File(model().getApplication<android.app.Application>().cacheDir, "invalid.pdf").apply { writeText("%PDF-broken") }
        compose.runOnIdle { model().save(Uri.fromFile(invalid)) }
        awaitIdle()
        assertTrue(model().state.value.documents.isEmpty())
        assertTrue(File(model().getApplication<android.app.Application>().filesDir, "documents").listFiles().orEmpty().isEmpty())
    }

    @Test fun scanRequestsScannerWithoutAnUpfrontModeDialog() {
        awaitIdle()
        var launches=0
        compose.activity.setContent {
            com.scanly.app.ui.theme.ScanlyTheme {
                com.scanly.app.ui.ScanlyApp(model(),onScan={ launches++ },onOpen={},onShare={},onExport={})
            }
        }
        compose.onNodeWithContentDescription("Muat ulang").assertDoesNotExist()
        compose.onNodeWithText("Scan Dokumen",useUnmergedTree=true).performClick()
        compose.onNodeWithText("Pilih mode scan").assertDoesNotExist()
        assertEquals(1,launches)
    }

    @Test fun cancelledAndMissingGoogleResultsClearBusyWithoutCreatingPdf() {
        awaitIdle()
        compose.runOnIdle {
            model().scannerReady()
            compose.activity.handleScannerResult(android.app.Activity.RESULT_CANCELED,null)
            assertEquals(com.scanly.app.ui.ScannerPhase.IDLE,model().state.value.scannerPhase)
            assertTrue(model().state.value.documents.isEmpty());assertNull(model().state.value.message)
            model().scannerReady()
            compose.activity.handleScannerResult(android.app.Activity.RESULT_OK,android.content.Intent())
            assertEquals(com.scanly.app.ui.ScannerPhase.IDLE,model().state.value.scannerPhase)
            assertTrue(model().state.value.documents.isEmpty())
            assertTrue(model().state.value.message!!.contains("tidak menghasilkan PDF"))
            model().scannerBusy()
            assertEquals(com.scanly.app.ui.ScannerPhase.PREPARING,model().state.value.scannerPhase)
            model().scannerIdle()
        }
    }

    @Test fun imageToolLaunchesInternalActivityAndCancellationReturnsIdle() {
        awaitIdle()
        compose.onNodeWithContentDescription("Gambar ke PDF").performClick()
        val shadow=org.robolectric.Shadows.shadowOf(compose.activity)
        val started=shadow.nextStartedActivityForResult
        assertEquals(com.scanly.app.tools.DocumentToolsActivity::class.java.name,started.intent.component!!.className)
        assertEquals("IMAGES_TO_PDF",started.intent.getStringExtra("mode"))
        compose.runOnIdle { shadow.receiveResult(started.intent,android.app.Activity.RESULT_CANCELED,null) }
        assertEquals(com.scanly.app.ui.ScannerPhase.IDLE,model().state.value.scannerPhase)
    }

    @Test fun homePdfToolsChooseTheRequestedDocument() {
        awaitIdle()
        compose.runOnIdle { model().save(Uri.fromFile(fixture())) }
        awaitIdle()
        val document = model().state.value.documents.single()
        var chosen: Pair<com.scanly.app.tools.ToolMode, String?>? = null
        compose.activity.setContent {
            com.scanly.app.ui.theme.ScanlyTheme {
                com.scanly.app.ui.ScanlyApp(model(), onScan = {}, onOpen = {}, onShare = {}, onExport = {},
                    onTools = { mode, pdf -> chosen = mode to pdf?.id })
            }
        }
        screenshot("home-layout")
        compose.onNodeWithContentDescription("PDF ke JPG/PNG").performClick()
        compose.onNodeWithText("Pilih PDF untuk ekspor gambar").assertIsDisplayed()
        compose.onNodeWithTag("tool-document-${document.id}").performClick()
        assertEquals(com.scanly.app.tools.ToolMode.PDF_TO_IMAGES to document.id, chosen)
        compose.onNodeWithContentDescription("Sematkan tanda tangan").performClick()
        compose.onNodeWithText("Pilih PDF untuk tanda tangan").assertIsDisplayed()
        compose.onNodeWithTag("tool-document-${document.id}").performClick()
        assertEquals(com.scanly.app.tools.ToolMode.SIGN_PDF to document.id, chosen)
    }

    @Test fun pdfToolsWithNoDocumentsOfferScanWithoutLaunchingAnInvalidTool() {
        awaitIdle()
        var launches = 0
        var scans = 0
        compose.activity.setContent {
            com.scanly.app.ui.theme.ScanlyTheme {
                com.scanly.app.ui.ScanlyApp(model(), onScan = { scans++ }, onOpen = {}, onShare = {}, onExport = {},
                    onTools = { _, _ -> launches++ })
            }
        }
        compose.onNodeWithContentDescription("PDF ke JPG/PNG").performClick()
        compose.onNodeWithText("Belum ada PDF yang bisa diproses.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Batal", substring = false).performClick()
        assertEquals(0, launches)
        compose.onNodeWithContentDescription("Sematkan tanda tangan").performClick()
        compose.onAllNodesWithText("Scan Dokumen", useUnmergedTree = true).onLast().performClick()
        assertEquals(1, scans)
        assertEquals(0, launches)
    }

    @Test fun productionFolderNavigationSurvivesActivityRecreation() {
        awaitIdle()
        compose.runOnIdle { model().createFolder("Arsip") }; awaitIdle()
        val folder = model().state.value.folders.single()
        compose.onNodeWithText("Folder", substring = false).performClick()
        compose.onNodeWithTag("folder-${folder.id}").performClick()
        compose.onNodeWithContentDescription("Kembali ke folder").assertExists()
        compose.activityRule.scenario.recreate(); awaitIdle()
        compose.onNodeWithContentDescription("Kembali ke folder").assertExists()
        compose.onNodeWithText("Folder ini masih kosong").assertExists()
        compose.onNodeWithContentDescription("Kembali ke folder").performClick()
        compose.onNodeWithTag("folder-list").assertExists()
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-mdpi")
    fun quickToolsAndFolderCardsRemainReachableOnSmallScreenWithLargeText() {
        awaitIdle()
        compose.activity.setContent {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(1f, 1.5f)) {
                com.scanly.app.ui.theme.ScanlyTheme {
                    com.scanly.app.ui.ScanlyApp(model(), onScan = {}, onOpen = {}, onShare = {}, onExport = {})
                }
            }
        }
        compose.onNodeWithContentDescription("Sematkan tanda tangan").performScrollTo().assertIsDisplayed()
        screenshot("home-small-large-font")
        compose.onNodeWithText("Folder", substring = false).performClick()
        compose.onNodeWithTag("folder-~").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Kembali ke folder").assertIsDisplayed()
        compose.onNodeWithText("Scan ke folder ini").performScrollTo().assertIsDisplayed()
    }

    @Test fun pullingDownReloadsNewDocumentsOnHomeAndDocumentsTab() {
        awaitIdle()
        val directory=File(model().getApplication<android.app.Application>().filesDir,"documents")
        val first=com.scanly.app.data.PdfStore(directory).save(fixture().inputStream())
        assertTrue(model().state.value.documents.isEmpty())
        compose.onNodeWithTag("document-refresh").performTouchInput { swipeDown(startY=10f,endY=height-10f,durationMillis=700) }
        compose.waitUntil(20_000) { model().state.value.documents.any { it.id==first.name } }
        compose.onNodeWithText("Dokumen").performClick()
        val second=com.scanly.app.data.PdfStore(directory).save(fixture().inputStream())
        compose.onNodeWithTag("document-refresh").performTouchInput { swipeDown(startY=10f,endY=height-10f,durationMillis=700) }
        compose.waitUntil(20_000) { model().state.value.documents.any { it.id==second.name } }
        assertEquals(2,model().state.value.documents.size)
    }
}
