package com.scanly.app.data

import java.io.File

data class Document(val file: File, val pageCount: Int?, val folderId: String? = null, val ocrPages: Int? = null, val ocrAt: Long? = null) {
    val id: String get() = file.name
    val name: String get() = file.nameWithoutExtension
    val modified: Long get() = file.lastModified()
    val size: Long get() = file.length()
}
