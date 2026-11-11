package com.mowtiie.mesgard.data.attachment

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.test.core.app.ApplicationProvider
import com.mowtiie.mesgard.domain.attachment.AttachmentRejection
import com.mowtiie.mesgard.domain.attachment.ImportResult
import com.mowtiie.mesgard.domain.attachment.MAX_ATTACHMENT_BYTES
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContentResolverAttachmentImporterTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val importer = ContentResolverAttachmentImporter(application, ImageReencoder(application))

    @Before
    fun registerProvider() {
        PickerProvider.files.clear()
        Robolectric.setupContentProvider(PickerProvider::class.java, AUTHORITY)
    }

    @Test
    fun `copies a document into app storage and reports its name, type and size`() = runTest {
        val source = pick("report.pdf", "application/pdf", "pdf-bytes")

        val result = importer.import(source, "attachment-1") as ImportResult.Imported

        assertEquals("report.pdf", result.fileName)
        assertEquals("application/pdf", result.mimeType)
        assertEquals(9L, result.sizeBytes)
        assertEquals("pdf-bytes", File(result.localPath).readText())
    }

    @Test
    fun `rejects a type that is not allowed`() = runTest {
        val source = pick("archive.zip", "application/zip", "zip")

        assertEquals(ImportResult.Rejected(AttachmentRejection.TYPE_NOT_ALLOWED), importer.import(source, "a"))
    }

    @Test
    fun `rejects a file larger than the limit before copying it`() = runTest {
        val source = pick("big.pdf", "application/pdf", "x", declaredSize = MAX_ATTACHMENT_BYTES + 1)

        assertEquals(ImportResult.Rejected(AttachmentRejection.TOO_LARGE), importer.import(source, "a"))
        assertFalse(File(application.noBackupFilesDir, "$ATTACHMENT_DIRECTORY/a").exists())
    }

    @Test
    fun `rejects a file whose type is unknown`() = runTest {
        val source = pick("mystery", null, "data")

        assertEquals(ImportResult.Rejected(AttachmentRejection.UNREADABLE), importer.import(source, "a"))
    }

    @Test
    fun `deletes a stored file`() = runTest {
        val result = importer.import(pick("a.txt", "text/plain", "text"), "attachment-2") as ImportResult.Imported

        importer.delete(result.localPath)

        assertFalse(File(result.localPath).exists())
    }

    @Test
    fun `stores files under the no backup directory`() = runTest {
        val result = importer.import(pick("a.txt", "text/plain", "text"), "attachment-3") as ImportResult.Imported

        assertTrue(result.localPath.startsWith(application.noBackupFilesDir.absolutePath))
    }

    private fun pick(name: String, mimeType: String?, content: String, declaredSize: Long? = null): String {
        val file = temporaryFolder.newFile().apply { writeText(content) }
        PickerProvider.files[name] = PickedFile(file, mimeType, declaredSize ?: file.length())
        return "content://$AUTHORITY/$name"
    }

    data class PickedFile(val file: File, val mimeType: String?, val size: Long)

    class PickerProvider : ContentProvider() {
        override fun onCreate() = true

        override fun getType(uri: Uri): String? = files[uri.lastPathSegment]?.mimeType

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor? {
            val picked = files[uri.lastPathSegment] ?: return null
            val cursor = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE))
            cursor.addRow(arrayOf<Any?>(uri.lastPathSegment, picked.size))
            return cursor
        }

        override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? =
            files[uri.lastPathSegment]?.let { ParcelFileDescriptor.open(it.file, ParcelFileDescriptor.MODE_READ_ONLY) }

        override fun insert(uri: Uri, values: ContentValues?): Uri? = null

        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0

        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0

        companion object {
            val files = mutableMapOf<String, PickedFile>()
        }
    }

    private companion object {
        const val AUTHORITY = "com.mowtiie.mesgard.test.picker"
    }
}
