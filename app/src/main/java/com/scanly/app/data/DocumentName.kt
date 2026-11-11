package com.scanly.app.data

/** One filename rule shared by the dialog and filesystem operations. */
object DocumentName {
    fun normalize(value: String): String = value.trim().let {
        if (it.endsWith(".pdf", ignoreCase = true)) it.dropLast(4).trim() else it
    }

    fun error(value: String): String? {
        val name = normalize(value)
        return when {
            name.isBlank() -> "Nama dokumen tidak boleh kosong."
            name.startsWith(".") -> "Nama dokumen tidak boleh diawali titik."
            name.any { it.isISOControl() || it in "/\\:*?\"<>|" } -> "Jangan gunakan garis miring, karakter kontrol, atau karakter khusus berkas."
            name.toByteArray(Charsets.UTF_8).size > 180 -> "Nama terlalu panjang. Gunakan nama yang lebih singkat."
            else -> null
        }
    }
}
