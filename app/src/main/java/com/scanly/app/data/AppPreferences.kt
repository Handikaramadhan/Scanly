package com.scanly.app.data

import android.content.Context
import androidx.core.content.edit

enum class ThemeMode(val label: String) { SYSTEM("Ikuti sistem"), LIGHT("Terang"), DARK("Gelap") }
enum class DocumentSort(val label: String) { NEWEST("Terbaru"), OLDEST("Terlama"), NAME("Nama A–Z") }

class AppPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
    fun theme(): ThemeMode = ThemeMode.entries.find { it.name == preferences.getString("theme", null) } ?: ThemeMode.SYSTEM
    fun setTheme(theme: ThemeMode) { preferences.edit { putString("theme", theme.name) } }
}
