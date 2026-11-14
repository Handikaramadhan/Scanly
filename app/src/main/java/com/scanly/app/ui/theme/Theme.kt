package com.scanly.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.scanly.app.data.ThemeMode

private val Light = lightColorScheme(
    primary = Color(0xFF275DCE), onPrimary = Color.White,
    primaryContainer = Color(0xFFE4ECFF), onPrimaryContainer = Color(0xFF123376),
    secondaryContainer = Color(0xFFE4ECFF), onSecondaryContainer = Color(0xFF123376),
    background = Color(0xFFF8F9FD), surface = Color(0xFFF8F9FD)
)
private val Dark = darkColorScheme(
    primary = Color(0xFFB0C6FF), onPrimary = Color(0xFF002D73),
    primaryContainer = Color(0xFF173E85), onPrimaryContainer = Color(0xFFDBE5FF),
    secondaryContainer = Color(0xFF173E85), onSecondaryContainer = Color(0xFFDBE5FF),
    background = Color(0xFF111318), surface = Color(0xFF111318)
)
@Composable
fun ScanlyTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
