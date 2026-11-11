package com.scanly.app.scanner

import android.app.Activity
import android.content.IntentSender
import com.google.android.gms.tasks.Task
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning

class ScannerLauncher internal constructor(private val prepare: ((Activity) -> Task<IntentSender>)? = null) {
    private val client by lazy {
        GmsDocumentScanning.getClient(
            GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(true)
                .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build()
        )
    }
    fun launch(activity: Activity, launcher: ActivityResultLauncher<IntentSenderRequest>, onReady: () -> Unit, onError: (String) -> Unit) {
        try {
            (prepare?.invoke(activity) ?: client.getStartScanIntent(activity))
                .addOnSuccessListener(activity) { sender ->
                    try {
                        onReady()
                        launcher.launch(IntentSenderRequest.Builder(sender).build())
                    } catch (_: Exception) { onError("Scanner tidak dapat dibuka. Ketuk Scan Dokumen untuk mencoba lagi.") }
                }
                .addOnFailureListener(activity) { error ->
                    onError(if (error is MlKitException && error.errorCode == MlKitException.UNSUPPORTED)
                        "Perangkat ini tidak mendukung scanner ML Kit. Diperlukan Google Play services yang kompatibel dan RAM yang cukup."
                    else "Scanner belum dapat disiapkan. Jika ini pemakaian pertama, hubungkan ke internet, periksa Google Play services, lalu ketuk Scan Dokumen untuk mencoba lagi.")
                }
        } catch (_: Exception) {
            onError("Scanner tidak tersedia. Periksa Google Play services di perangkat, lalu coba lagi.")
        }
    }
}
