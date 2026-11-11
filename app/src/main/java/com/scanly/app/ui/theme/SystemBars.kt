package com.scanly.app.ui.theme

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.scanly.app.data.ThemeMode

@Composable
fun MatchSystemBars(activity: ComponentActivity, mode: ThemeMode) {
    val systemDark=isSystemInDarkTheme()
    val dark=when(mode) { ThemeMode.SYSTEM -> systemDark; ThemeMode.LIGHT -> false; ThemeMode.DARK -> true }
    DisposableEffect(dark,activity) {
        activity.enableEdgeToEdge(
            statusBarStyle=SystemBarStyle.auto(Color.TRANSPARENT,Color.TRANSPARENT) { dark },
            navigationBarStyle=SystemBarStyle.auto(0xE6FFFFFF.toInt(),0x801B1B1B.toInt()) { dark })
        onDispose { }
    }
}
