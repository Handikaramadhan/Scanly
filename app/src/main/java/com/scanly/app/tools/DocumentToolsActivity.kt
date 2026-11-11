package com.scanly.app.tools

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.scanly.app.data.AppPreferences
import com.scanly.app.ui.theme.MatchSystemBars
import com.scanly.app.ui.theme.ScanlyTheme
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DocumentToolsActivity: ComponentActivity() {
    private val model: DocumentToolsModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);enableEdgeToEdge()
        val mode=ToolMode.entries.find { it.name==intent.getStringExtra("mode") } ?: ToolMode.IMAGES_TO_PDF
        model.initialize(mode,intent.getStringExtra("documentId"),intent.getStringExtra("folderId"))
        setContent {
            val theme=remember { AppPreferences(application).theme() };MatchSystemBars(this,theme)
            val state by model.state.collectAsStateWithLifecycle()
            LaunchedEffect(state.completedId) { if(state.completedId!=null) { setResult(Activity.RESULT_OK);finish() } }
            ScanlyTheme(theme) { ToolsScreen(state,model,onClose={ finish() }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolsScreen(state: ToolState,model: DocumentToolsModel,onClose: ()->Unit) {
    var draw by rememberSaveable { mutableStateOf(false) }
    var exit by rememberSaveable { mutableStateOf(false) }
    var formatName by rememberSaveable { mutableStateOf(PictureFormat.JPG.name) }
    var exportPage by rememberSaveable { mutableStateOf<Int?>(null) }
    var picking by rememberSaveable { mutableStateOf(false) }
    val format=PictureFormat.valueOf(formatName)
    val exportJpg=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")) { picking=false;it?.let { uri -> model.export(uri,PictureFormat.JPG,exportPage) } }
    val exportPng=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { picking=false;it?.let { uri -> model.export(uri,PictureFormat.PNG,exportPage) } }
    val exportZip=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { picking=false;it?.let { uri -> model.export(uri,PictureFormat.valueOf(formatName),null) } }
    val images=rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { model.importImages(it) }
    val ink=rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { it?.let(model::importSignature) }
    val close={ if(state.busy) model.cancel() else if(state.images.isNotEmpty() || state.signature!=null) exit=true else onClose() }
    BackHandler { close() }
    Scaffold(topBar={ TopAppBar(title={ Text(when(state.mode) {
        ToolMode.IMAGES_TO_PDF -> "Gambar ke PDF";ToolMode.PDF_TO_IMAGES -> "PDF ke gambar";ToolMode.SIGN_PDF -> "Tanda tangan PDF"
    }) },navigationIcon={ IconButton(onClick=close) { Icon(Icons.Outlined.Close,"Tutup alat") } }) }) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
        val compact=maxHeight<500.dp && state.mode!=ToolMode.IMAGES_TO_PDF
        Column(Modifier.fillMaxSize().then(if(compact) Modifier.verticalScroll(rememberScrollState()) else Modifier).padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            if(state.busy || state.loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(state.progress ?: "Menyiapkan…",style=MaterialTheme.typography.bodySmall)
                if(!state.loading) TextButton(onClick=model::cancel) { Text("Batalkan proses") }
            }
            val enabled=!state.busy && !state.loading && !picking
            if(state.mode==ToolMode.IMAGES_TO_PDF) {
                Text("${state.images.size} gambar · urutan gambar menjadi halaman PDF",style=MaterialTheme.typography.bodySmall)
                LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    if(state.images.isEmpty()) item { Text("Pilih gambar dari galeri, lalu atur urutannya sebelum membuat PDF.",Modifier.padding(vertical=28.dp)) }
                    itemsIndexed(state.images,key={ _,file -> file.name }) { index,file ->
                        val preview by fileBitmap(file,180)
                        OutlinedCard(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically) {
                            preview?.let { Image(it.asImageBitmap(),null,Modifier.size(56.dp),contentScale=ContentScale.Fit) }
                            Text("${index+1}",Modifier.weight(1f).padding(8.dp))
                            IconButton(enabled=enabled && index>0,onClick={ model.moveImage(index,-1) }) { Icon(Icons.Outlined.KeyboardArrowUp,"Naik halaman ${index+1}") }
                            IconButton(enabled=enabled && index<state.images.lastIndex,onClick={ model.moveImage(index,1) }) { Icon(Icons.Outlined.KeyboardArrowDown,"Turun halaman ${index+1}") }
                            IconButton(enabled=enabled,onClick={ model.removeImage(index) }) { Icon(Icons.Outlined.DeleteOutline,"Hapus gambar ${index+1}") }
                        } }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(bottom=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled=enabled,onClick={ images.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },modifier=Modifier.weight(1f)) { Text("Tambah gambar") }
                    Button(enabled=enabled && state.images.isNotEmpty(),onClick=model::createPdf,modifier=Modifier.weight(1f)) { Text("Buat PDF") }
                }
            } else {
                val page by produceState<Bitmap?>(null,state.source,state.page) {
                    value=null
                    value=state.source?.let { file -> withContext(Dispatchers.IO) { runCatching { PdfPages(file).use { it.render(state.page,1400).bitmap } }.getOrNull() } }
                }
                val signature by fileBitmap(state.signature,1200)
                PdfToolPreview(page,if(state.mode==ToolMode.SIGN_PDF) signature else null,state.position,enabled,
                    onPosition=model::place,modifier=Modifier.fillMaxWidth().then(if(compact) Modifier.height(220.dp) else Modifier.weight(1f)))
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                    TextButton(enabled=enabled && state.page>0,onClick={ model.selectPage(-1) }) { Text("Sebelumnya") }
                    Text("${if(state.pages>0) state.page+1 else 0} / ${state.pages}",style=MaterialTheme.typography.labelLarge)
                    TextButton(enabled=enabled && state.page<state.pages-1,onClick={ model.selectPage(1) }) { Text("Berikutnya") }
                }
                if(state.mode==ToolMode.PDF_TO_IMAGES) {
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { PictureFormat.entries.forEach { option ->
                        FilterChip(selected=format==option,enabled=enabled,onClick={ formatName=option.name },label={ Text(option.label) })
                    } }
                    Button(enabled=enabled && state.pages>0,onClick={
                        exportPage=state.page;picking=true
                        try { if(format==PictureFormat.JPG) exportJpg.launch("halaman-${state.page+1}.jpg") else exportPng.launch("halaman-${state.page+1}.png") }
                        catch(_: Exception) { picking=false;model.message("Pemilih lokasi ekspor tidak tersedia.") }
                    },modifier=Modifier.fillMaxWidth()) { Text("Ekspor halaman ini (${format.label})") }
                    OutlinedButton(enabled=enabled && state.pages>0,onClick={ exportPage=null;picking=true
                        try { exportZip.launch("halaman-${format.extension}.zip") } catch(_: Exception) { picking=false;model.message("Pemilih lokasi ekspor tidak tersedia.") }
                    },modifier=Modifier.fillMaxWidth().padding(bottom=12.dp)) { Text("Semua halaman (${format.label} dalam ZIP)") }
                } else {
                    Text("Ketuk/geser di halaman untuk posisi. TTD ditempel pada halaman ${state.page+1}.",style=MaterialTheme.typography.bodySmall)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(enabled=enabled,onClick={ draw=true },modifier=Modifier.weight(1f)) { Text("Gambar TTD") }
                        OutlinedButton(enabled=enabled,onClick={ ink.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },modifier=Modifier.weight(1f)) { Text("Dari gambar") }
                    }
                    if(state.rawSignature!=null) {
                        Text("Hapus latar otomatis · paling cocok untuk tinta di kertas. Fokuskan foto pada tanda tangan.",style=MaterialTheme.typography.labelSmall)
                        Slider(value=state.strength.toFloat(),onValueChange={ model.setStrength(it.toInt()) },onValueChangeFinished=model::reclean,
                            valueRange=0f..45f,enabled=enabled,modifier=Modifier.semantics { contentDescription="Pembersih latar tanda tangan" })
                    }
                    if(state.signature!=null) {
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            Text("Ukuran",Modifier.width(64.dp),style=MaterialTheme.typography.labelMedium)
                            Slider(value=state.position.width.coerceIn(.08f,.8f),onValueChange={ model.place(state.position.copy(width=it)) },valueRange=.08f.. .8f,enabled=enabled,
                                modifier=Modifier.semantics { contentDescription="Ukuran tanda tangan" })
                        }
                    }
                    Text("Disimpan sebagai salinan gambar PDF; teks/formulir interaktif tidak dipertahankan. PDF asli tetap ada.",style=MaterialTheme.typography.labelSmall)
                    Button(enabled=enabled && state.signature!=null && state.pages>0,onClick=model::createPdf,modifier=Modifier.fillMaxWidth().padding(bottom=12.dp)) { Text("Simpan PDF bertanda tangan") }
                }
            }
        }
        }
    }
    if(draw) DrawSignatureDialog(onDismiss={ draw=false },onApply={ bitmap -> draw=false;model.drawnSignature(bitmap) })
    if(exit) AlertDialog(onDismissRequest={ exit=false },title={ Text("Keluar tanpa menyimpan?") },text={ Text("Gambar/tanda tangan sesi ini belum dibuat menjadi PDF. Dokumen asli tetap ada.") },
        confirmButton={ TextButton(onClick={ exit=false;onClose() }) { Text("Keluar") } },dismissButton={ TextButton(onClick={ exit=false }) { Text("Lanjut edit") } })
    state.message?.let { message -> AlertDialog(onDismissRequest=model::dismissMessage,title={ Text("Alat dokumen") },text={ Text(message) },confirmButton={ TextButton(onClick=model::dismissMessage) { Text("Oke") } }) }
}

@Composable
private fun fileBitmap(file: File?,edge: Int): State<Bitmap?> = produceState<Bitmap?>(null,file) {
    value=null
    value=file?.let { source -> withContext(Dispatchers.IO) {
        val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true };BitmapFactory.decodeFile(source.path,bounds)
        var sample=1;while(maxOf(bounds.outWidth,bounds.outHeight)/sample>edge) sample*=2
        BitmapFactory.decodeFile(source.path,BitmapFactory.Options().apply { inSampleSize=sample })
    } }
}

@Composable
internal fun PdfToolPreview(page: Bitmap?,ink: Bitmap?,position: SignaturePosition,enabled: Boolean,
                            onPosition: (SignaturePosition)->Unit,modifier: Modifier=Modifier) {
    val current by rememberUpdatedState(position)
    Canvas(modifier.background(MaterialTheme.colorScheme.surfaceVariant).testTag("tool-page-preview")
        .semantics { contentDescription=if(ink==null) "Pratinjau halaman PDF" else "Posisi tanda tangan pada halaman" }
        .pointerInput(page,ink,enabled) {
            if(page!=null && ink!=null && enabled) {
                fun place(point: Offset) {
                    val scale=minOf(size.width.toFloat()/page.width,size.height.toFloat()/page.height)
                    val w=page.width*scale;val h=page.height*scale;val left=(size.width-w)/2;val top=(size.height-h)/2
                    val p=current.bounded(page.width.toFloat()/page.height,ink.width.toFloat()/ink.height)
                    val inkH=p.width*page.width/page.height*ink.height/ink.width
                    onPosition(p.copy(x=(point.x-left)/w-p.width/2,y=(point.y-top)/h-inkH/2).bounded(page.width.toFloat()/page.height,ink.width.toFloat()/ink.height))
                }
                awaitEachGesture { val down=awaitFirstDown();down.consume();place(down.position);drag(down.id) { change -> change.consume();place(change.position) } }
            }
        }) {
        page?.let { image ->
            val scale=minOf(size.width/image.width,size.height/image.height)
            val w=image.width*scale;val h=image.height*scale;val left=(size.width-w)/2;val top=(size.height-h)/2
            drawImage(image.asImageBitmap(),dstOffset=IntOffset(left.toInt(),top.toInt()),dstSize=IntSize(w.toInt().coerceAtLeast(1),h.toInt().coerceAtLeast(1)))
            ink?.let { signature ->
                val p=position.bounded(image.width.toFloat()/image.height,signature.width.toFloat()/signature.height)
                val iw=p.width*w;val ih=iw*signature.height/signature.width
                drawImage(signature.asImageBitmap(),dstOffset=IntOffset((left+p.x*w).toInt(),(top+p.y*h).toInt()),dstSize=IntSize(iw.toInt().coerceAtLeast(1),ih.toInt().coerceAtLeast(1)))
                drawRect(Color(0xFF4979CF),Offset(left+p.x*w,top+p.y*h),androidx.compose.ui.geometry.Size(iw,ih),style=Stroke(1.dp.toPx()))
            }
        }
    }
}

data class InkPoint(val x: Float,val y: Float)
internal object SignatureDrawing {
    fun bitmap(strokes: List<List<InkPoint>>): Bitmap {
        require(strokes.any { it.isNotEmpty() }) { "Buat coretan tanda tangan terlebih dahulu." }
        val bitmap=Bitmap.createBitmap(1000,500,Bitmap.Config.ARGB_8888)
        val canvas=android.graphics.Canvas(bitmap)
        val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color=android.graphics.Color.BLACK;style=android.graphics.Paint.Style.STROKE;strokeWidth=6f;strokeCap=android.graphics.Paint.Cap.ROUND;strokeJoin=android.graphics.Paint.Join.ROUND
        }
        strokes.forEach { points -> if(points.isNotEmpty()) {
            val path=android.graphics.Path();path.moveTo(points[0].x*1000,points[0].y*500)
            points.drop(1).forEach { path.lineTo(it.x*1000,it.y*500) }
            if(points.size==1) canvas.drawCircle(points[0].x*1000,points[0].y*500,3f,android.graphics.Paint(paint).apply { style=android.graphics.Paint.Style.FILL }) else canvas.drawPath(path,paint)
        } }
        return bitmap
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DrawSignatureDialog(onDismiss: ()->Unit,onApply: (Bitmap)->Unit) {
    val saver=remember { Saver<List<List<InkPoint>>,FloatArray>(save={ strokes -> strokes.flatMap { points -> points.flatMap { listOf(it.x,it.y) }+listOf(Float.NaN,Float.NaN) }.toFloatArray() },restore={ data ->
        val strokes=mutableListOf<List<InkPoint>>();var points=mutableListOf<InkPoint>()
        for(i in data.indices step 2) if(i+1<data.size) { if(data[i].isNaN()) { if(points.isNotEmpty()) strokes.add(points);points=mutableListOf() } else points.add(InkPoint(data[i],data[i+1])) }
        if(points.isNotEmpty()) strokes.add(points);strokes
    }) }
    var strokes by rememberSaveable(stateSaver=saver) { mutableStateOf<List<List<InkPoint>>>(emptyList()) }
    Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize()) { Column(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("Gambar tanda tangan",style=MaterialTheme.typography.titleLarge)
            Text("Coret di area putih. Latar hasil tanda tangan transparan.",style=MaterialTheme.typography.bodySmall)
            Box(Modifier.weight(1f),contentAlignment=Alignment.Center) {
                Canvas(Modifier.fillMaxWidth().aspectRatio(2f).background(Color.White).border(1.dp,Color.Gray).testTag("signature-pad")
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down=awaitFirstDown();down.consume()
                            fun point(p: Offset)=InkPoint((p.x/size.width).coerceIn(0f,1f),(p.y/size.height).coerceIn(0f,1f))
                            if(strokes.sumOf { it.size }<10000) {
                                strokes=strokes+listOf(listOf(point(down.position)))
                                drag(down.id) { change ->
                                    change.consume()
                                    if(strokes.sumOf { it.size }<10000) strokes=strokes.dropLast(1)+listOf(strokes.last()+point(change.position))
                                }
                            }
                        }
                    }) {
                    strokes.forEach { points -> if(points.isNotEmpty()) {
                        val path=Path().apply { moveTo(points[0].x*size.width,points[0].y*size.height);points.drop(1).forEach { lineTo(it.x*size.width,it.y*size.height) } }
                        if(points.size==1) drawCircle(Color.Black,size.width*.003f,Offset(points[0].x*size.width,points[0].y*size.height))
                        else drawPath(path,Color.Black,style=Stroke(size.width*.006f,cap=androidx.compose.ui.graphics.StrokeCap.Round,join=androidx.compose.ui.graphics.StrokeJoin.Round))
                    } }
                }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                TextButton(enabled=strokes.isNotEmpty(),onClick={ strokes=strokes.dropLast(1) }) { Text("Urungkan") }
                TextButton(enabled=strokes.isNotEmpty(),onClick={ strokes=emptyList() }) { Text("Bersihkan") }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick=onDismiss,modifier=Modifier.weight(1f)) { Text("Batal") }
                Button(enabled=strokes.isNotEmpty(),onClick={ onApply(SignatureDrawing.bitmap(strokes)) },modifier=Modifier.weight(1f)) { Text("Gunakan TTD") }
            }
        } }
    }
}
