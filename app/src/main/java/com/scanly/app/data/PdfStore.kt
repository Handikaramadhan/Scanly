package com.scanly.app.data

import java.io.File
import java.io.InputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/** Commit only complete PDF copies; interrupted copies never appear in the list. */
class PdfStore(private val directory: File, private val validate: (File) -> Unit = {}) {
    init { check(directory.isDirectory || directory.mkdirs()) { "Penyimpanan tidak tersedia." } }

    fun list(): List<File> = (directory.listFiles() ?: throw IOException("Daftar dokumen tidak dapat dibaca."))
        .filter { it.isFile && it.extension == "pdf" }
        .sortedByDescending { it.lastModified() }

    fun save(source: InputStream): File = source.use { input ->
        saveGenerated { partial -> partial.outputStream().use { output -> input.copyTo(output); output.fd.sync() } }
    }

    fun saveGenerated(documentId: String? = null, writer: (File) -> Unit): File {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(Date())
        val name = documentId ?: "Scan-$stamp-${UUID.randomUUID()}.pdf"
        require(documentId == null || Regex("Scan-[0-9a-f-]{36}\\.pdf").matches(name))
        val target = requireOwned(File(directory, name))
        fun verify(file: File) {
            val header = ByteArray(5)
            val count = file.inputStream().use { it.read(header) }
            if (count != 5 || !header.contentEquals("%PDF-".toByteArray(Charsets.US_ASCII)))
                throw IOException("Hasil scan bukan PDF yang valid.")
            validate(file)
        }
        // A restored scanner session can retry after a completed commit without duplicating/overwriting it.
        if (documentId != null && target.exists()) { verify(target); return target }
        val partial = File.createTempFile(".scanly-", ".partial", directory)
        try {
            writer(partial)
            verify(partial)
            if (!partial.renameTo(target)) throw IOException("PDF tidak dapat disimpan.")
            return target
        } finally { partial.delete() }
    }

    fun requireOwned(file: File): File {
        require(file.canonicalFile.parentFile == directory.canonicalFile && file.extension == "pdf") { "Identitas dokumen tidak valid." }
        return file
    }

    fun resolve(documentId: String): File {
        require(documentId == File(documentId).name) { "Identitas dokumen tidak valid." }
        val file = requireOwned(File(directory, documentId))
        require(file.isFile) { "Dokumen asal tidak tersedia." }
        return file
    }

    fun rename(file: File, requestedName: String): File {
        val source = requireOwned(file)
        require(source.isFile) { "Dokumen asal tidak tersedia." }
        require(DocumentName.error(requestedName) == null) {
            DocumentName.error(requestedName) ?: "Nama tidak valid."
        }
        val target = File(directory, DocumentName.normalize(requestedName) + ".pdf")
        if (source == target) return source
        require((directory.listFiles() ?: throw IOException("Daftar dokumen tidak dapat dibaca.")).none {
            it != source && it.name.equals(target.name, ignoreCase = true)
        }) { "Nama ini sudah dipakai. Pilih nama lain." }
        if (!source.renameTo(target)) throw IOException("Nama dokumen tidak dapat diubah.")
        return target
    }

    fun delete(file: File) {
        val owned = requireOwned(file)
        if (owned.exists() && !owned.delete()) throw IOException("Dokumen tidak dapat dihapus.")
    }
}
