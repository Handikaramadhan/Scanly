package com.scanly.app.data

import java.util.Locale

/** Stable values persisted in the catalogue; never depend on icon resource numbers. */
object FolderAppearance {
    const val DEFAULT_COLOR = "#275DCE"
    const val DEFAULT_ICON = "folder"
    fun color(value: String): String {
        val normalized = value.trim().uppercase(Locale.ROOT)
        require(Regex("#[0-9A-F]{6}").matches(normalized)) { "Warna harus berupa kode #RRGGBB, misalnya #275DCE." }
        return normalized
    }
    fun icon(value: String): String {
        require(FolderIconKey.entries.any { it.id == value }) { "Pilih ikon folder yang tersedia." }
        return value
    }
}

enum class FolderIconKey(val id: String, val label: String) {
    FOLDER("folder", "Folder"), WORK("work", "Kerja"), SCHOOL("school", "Sekolah"),
    RECEIPT("receipt", "Tagihan"), PERSON("person", "Pribadi"), HOME("home", "Rumah"),
    HEALTH("health", "Kesehatan"), TRAVEL("travel", "Perjalanan"), PHOTO("photo", "Foto"), STAR("star", "Favorit")
}
