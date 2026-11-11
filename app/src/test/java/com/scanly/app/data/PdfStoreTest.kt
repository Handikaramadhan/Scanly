package com.scanly.app.data

import java.io.File
import java.io.IOException
import java.io.InputStream
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PdfStoreTest {
    @get:Rule val temporary = TemporaryFolder()
    private fun pdf() = "%PDF-1.4\nfixture".byteInputStream()

    @Test fun savesDistinctFilesAndSurvivesNewStore() {
        val dir = temporary.newFolder()
        val store = PdfStore(dir)
        val first = store.save(pdf())
        val second = store.save(pdf())
        assertNotEquals(first.name, second.name)
        assertEquals(2, PdfStore(dir).list().size)
        assertEquals("%PDF-1.4\nfixture", first.readText())
    }
    @Test fun failedCopyLeavesNoDocumentOrPartialFile() {
        val dir = temporary.newFolder()
        val broken = object : InputStream() { override fun read(): Int = throw IOException("disk/source failure") }
        assertThrows(IOException::class.java) { PdfStore(dir).save(broken) }
        assertTrue(dir.listFiles().orEmpty().isEmpty())
    }
    @Test fun rejectsNonPdfAndCleansPartial() {
        val dir = temporary.newFolder()
        assertThrows(IOException::class.java) { PdfStore(dir).save("image".byteInputStream()) }
        assertTrue(dir.listFiles().orEmpty().isEmpty())
    }
    @Test fun cannotDeleteOutsideDocumentDirectory() {
        val outside = temporary.newFile("outside.pdf")
        assertThrows(IllegalArgumentException::class.java) { PdfStore(temporary.newFolder()).delete(outside) }
        assertTrue(outside.exists())
    }
    @Test fun listsOnlyCommittedPdfsAndDeletesChosenFile() {
        val dir = temporary.newFolder()
        File(dir, ".interrupted.partial").writeText("unfinished")
        val store = PdfStore(dir)
        val saved = store.save(pdf())
        assertEquals(listOf(saved), store.list())
        store.delete(saved)
        assertTrue(store.list().isEmpty())
    }

    @Test fun renamePreservesContentAndTimestampAcrossReload() {
        val dir = temporary.newFolder()
        val store = PdfStore(dir)
        val original = store.save(pdf())
        val before = original.readBytes()
        val modified = original.lastModified()
        val renamed = store.rename(original, "  Catatan rapat.PDF  ")
        assertEquals("Catatan rapat.pdf", renamed.name)
        assertArrayEquals(before, renamed.readBytes())
        assertEquals(modified, renamed.lastModified())
        assertFalse(original.exists())
        assertEquals(listOf(renamed), PdfStore(dir).list())
    }

    @Test fun duplicateRenameNeverOverwritesAnotherPdf() {
        val store = PdfStore(temporary.newFolder())
        val first = store.rename(store.save(pdf()), "Invoice")
        val second = store.save("%PDF-1.4\nsecond".byteInputStream())
        val firstBytes = first.readBytes()
        val secondBytes = second.readBytes()
        assertThrows(IllegalArgumentException::class.java) { store.rename(second, "invoice.pdf") }
        assertArrayEquals(firstBytes, first.readBytes())
        assertArrayEquals(secondBytes, second.readBytes())
        assertEquals(2, store.list().size)
    }

    @Test fun invalidNamesLeaveSourceIntact() {
        val store = PdfStore(temporary.newFolder())
        val source = store.save(pdf())
        listOf("", "  ", ".pdf", "..", "../outside", "a/b", "a\\b", "a\nname", "a?name", "é".repeat(91)).forEach { name ->
            assertThrows(IllegalArgumentException::class.java) { store.rename(source, name) }
            assertTrue(source.exists())
        }
        assertEquals(1, store.list().size)
    }

    @Test fun unicodeAndSameNameRenamesAreSupported() {
        val store = PdfStore(temporary.newFolder())
        val renamed = store.rename(store.save(pdf()), "Résumé 日本語")
        assertEquals("Résumé 日本語.pdf", renamed.name)
        assertEquals(renamed, store.rename(renamed, renamed.name))
        assertEquals(1, store.list().size)
    }

    @Test fun renameCannotMoveAnOutsideFile() {
        val outside = temporary.newFile("outside.pdf")
        val store = PdfStore(temporary.newFolder())
        assertThrows(IllegalArgumentException::class.java) { store.rename(outside, "inside") }
        assertTrue(outside.exists())
        assertTrue(store.list().isEmpty())
    }

    @Test fun missingSourceAndConflictingDirectoryAreRejected() {
        val dir = temporary.newFolder()
        val store = PdfStore(dir)
        assertThrows(IllegalArgumentException::class.java) { store.rename(File(dir, "missing.pdf"), "new") }
        File(dir, "existing.pdf").mkdir()
        val source = store.save(pdf())
        assertThrows(IllegalArgumentException::class.java) { store.rename(source, "existing") }
        assertTrue(source.exists())
        assertTrue(File(dir, "existing.pdf").isDirectory)
    }

    @Test fun rendererValidationFailureLeavesNoDocumentOrPartial() {
        val dir = temporary.newFolder()
        val store = PdfStore(dir) { throw IOException("corrupt PDF") }
        assertThrows(IOException::class.java) { store.save(pdf()) }
        assertTrue(dir.listFiles().orEmpty().isEmpty())
    }

    @Test fun sourceClosesEvenWhenTemporaryFileCannotBeCreated() {
        val dir = temporary.newFolder()
        val store = PdfStore(dir)
        assertTrue(dir.delete())
        var closed = false
        val input = object : InputStream() {
            override fun read(): Int = -1
            override fun close() { closed = true }
        }
        assertThrows(IOException::class.java) { store.save(input) }
        assertTrue(closed)
    }

    @Test fun exportIdResolutionSurvivesNewStoreAndRejectsTraversal() {
        val dir = temporary.newFolder()
        val file = PdfStore(dir).save(pdf())
        assertEquals(file, PdfStore(dir).resolve(file.name))
        assertThrows(IllegalArgumentException::class.java) { PdfStore(dir).resolve("../outside.pdf") }
        assertThrows(IllegalArgumentException::class.java) { PdfStore(dir).resolve("missing.pdf") }
    }

    @Test fun generatedSessionPdfRetriesDoNotDuplicateOrOverwrite() {
        val store=PdfStore(temporary.newFolder())
        val id="Scan-12345678-1234-1234-1234-123456789abc.pdf"
        val first=store.saveGenerated(id) { it.writeText("%PDF-1.4\nfirst") }
        val second=store.saveGenerated(id) { fail("Committed session must not be rewritten") }
        assertEquals(first,second); assertEquals(1,store.list().size)
        assertEquals("%PDF-1.4\nfirst",second.readText())
    }
    @Test fun failedGeneratedPdfLeavesNoCommittedDocument() {
        val directory=temporary.newFolder(); val store=PdfStore(directory)
        assertThrows(IOException::class.java) {
            store.saveGenerated { it.writeText("%PDF-"); throw IOException("disk full") }
        }
        assertTrue(directory.listFiles().orEmpty().isEmpty())
    }
}
