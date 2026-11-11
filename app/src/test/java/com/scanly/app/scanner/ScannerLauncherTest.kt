package com.scanly.app.scanner

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.os.Looper
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.mlkit.common.MlKitException
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class ScannerLauncherTest {
    private class Launcher(private val fail: Boolean=false): ActivityResultLauncher<IntentSenderRequest>() {
        var request: IntentSenderRequest?=null
        override fun launch(input: IntentSenderRequest,options: ActivityOptionsCompat?) {
            if(fail) throw IllegalStateException("Launch unavailable")
            request=input
        }
        override fun unregister() {}
        override val contract: ActivityResultContract<IntentSenderRequest,*> = ActivityResultContracts.StartIntentSenderForResult()
    }
    private fun sender(activity: Activity): IntentSender = PendingIntent.getActivity(activity,1,Intent(activity,Activity::class.java),PendingIntent.FLAG_IMMUTABLE).intentSender
    @Test fun preparesThenLaunchesGoogleIntentSenderAndReportsReady() {
        val activity=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val task=TaskCompletionSource<IntentSender>();val launcher=Launcher()
            var ready=0;var error: String?=null
            ScannerLauncher { task.task }.launch(activity.get(),launcher,{ ready++ },{ error=it })
            assertNull(launcher.request);assertEquals(0,ready)
            val sender=sender(activity.get());task.setResult(sender);shadowOf(Looper.getMainLooper()).idle()
            assertEquals(sender,launcher.request!!.intentSender);assertEquals(1,ready);assertNull(error)
        } finally { activity.pause().stop().destroy() }
    }
    @Test fun unsupportedDeviceHasSpecificMessageAndDoesNotLaunch() {
        val activity=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val task=TaskCompletionSource<IntentSender>();val launcher=Launcher();var error=""
            ScannerLauncher { task.task }.launch(activity.get(),launcher,{}, { error=it })
            task.setException(MlKitException("Unsupported",MlKitException.UNSUPPORTED));shadowOf(Looper.getMainLooper()).idle()
            assertTrue(error.contains("tidak mendukung"));assertNull(launcher.request)
        } finally { activity.pause().stop().destroy() }
    }
    @Test fun downloadFailureExplainsFirstUseAndRetry() {
        val activity=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val task=TaskCompletionSource<IntentSender>();var error=""
            ScannerLauncher { task.task }.launch(activity.get(),Launcher(),{}, { error=it })
            task.setException(IllegalStateException("Offline"));shadowOf(Looper.getMainLooper()).idle()
            assertTrue(error.contains("internet"));assertTrue(error.contains("mencoba lagi"))
        } finally { activity.pause().stop().destroy() }
    }
    @Test fun launchFailureReturnsErrorWithoutEscapingCallback() {
        val activity=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            val task=TaskCompletionSource<IntentSender>();var error=""
            ScannerLauncher { task.task }.launch(activity.get(),Launcher(fail=true),{}, { error=it })
            task.setResult(sender(activity.get()));shadowOf(Looper.getMainLooper()).idle()
            assertTrue(error.contains("tidak dapat dibuka"))
        } finally { activity.pause().stop().destroy() }
    }
    @Test fun stoppedActivityDoesNotReceiveLatePreparationResult() {
        val activity=Robolectric.buildActivity(Activity::class.java).setup()
        val task=TaskCompletionSource<IntentSender>();val launcher=Launcher();var ready=0
        val sender=sender(activity.get())
        ScannerLauncher { task.task }.launch(activity.get(),launcher,{ ready++ },{})
        activity.pause().stop()
        task.setResult(sender);shadowOf(Looper.getMainLooper()).idle()
        assertEquals(0,ready);assertNull(launcher.request);activity.destroy()
    }
    @Test fun synchronousPreparationFailureIsReported() {
        val activity=Robolectric.buildActivity(Activity::class.java).setup()
        try {
            var error=""
            ScannerLauncher { throw IllegalStateException("Missing services") }.launch(activity.get(),Launcher(),{}, { error=it })
            assertTrue(error.contains("Google Play services"))
        } finally { activity.pause().stop().destroy() }
    }
}
