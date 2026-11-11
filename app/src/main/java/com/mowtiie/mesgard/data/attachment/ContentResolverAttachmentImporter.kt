package com.mowtiie.mesgard.data.attachment

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import com.mowtiie.mesgard.domain.attachment.ALLOWED_ATTACHMENT_TYPES
import com.mowtiie.mesgard.domain.attachment.AttachmentImporter
import com.mowtiie.mesgard.domain.attachment.AttachmentRejection
import com.mowtiie.mesgard.domain.attachment.ImportResult
import com.mowtiie.mesgard.domain.attachment.MAX_ATTACHMENT_BYTES
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val DEFAULT_FILE_NAME = "attachment"
private const val MAX_FILE_NAME_LENGTH = 255
private val RE_ENCODED_IMAGE_TYPES = setOf("image/jpeg", "image/png", "image/webp")

/** Animated GIFs and documents are copied unchanged. Photos go through [ImageReencoder]. */
internal class ContentResolverAttachmentImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageReencoder: ImageReencoder,
) : AttachmentImporter {
    override suspend fun import(uri: String, attachmentId: String): ImportResult = withContext(Dispatchers.IO) {
        val source = Uri.parse(uri)
        val mimeType = context.contentResolver.getType(source)
        val declaredSize = readSize(source)
        when {
            mimeType == null -> ImportResult.Rejected(AttachmentRejection.UNREADABLE)
            mimeType !in ALLOWED_ATTACHMENT_TYPES -> ImportResult.Rejected(AttachmentRejection.TYPE_NOT_ALLOWED)
            declaredSize != null && declaredSize > MAX_ATTACHMENT_BYTES ->
                ImportResult.Rejected(AttachmentRejection.TOO_LARGE)
            else -> copy(source, mimeType, attachmentId)
        }
    }

    override fun delete(localPath: String) {
        File(localPath).delete()
    }

    private fun copy(source: Uri, mimeType: String, attachmentId: String): ImportResult {
        val target = File(directory(), attachmentId)
        return try {
            if (mimeType in RE_ENCODED_IMAGE_TYPES) {
                imageReencoder.reencode(source, mimeType, target)
            } else {
                writeCopy(source, target)
            }
            describe(source, mimeType, target)
        } catch (_: IOException) {
            target.delete()
            ImportResult.Rejected(AttachmentRejection.UNREADABLE)
        }
    }

    private fun describe(source: Uri, mimeType: String, target: File): ImportResult {
        if (target.length() <= MAX_ATTACHMENT_BYTES) {
            return ImportResult.Imported(readName(source), mimeType, target.length(), target.absolutePath)
        }
        target.delete()
        return ImportResult.Rejected(AttachmentRejection.TOO_LARGE)
    }

    private fun writeCopy(source: Uri, target: File) {
        val input = context.contentResolver.openInputStream(source) ?: throw IOException("Cannot open $source")
        input.use { stream -> target.outputStream().use { stream.copyTo(it) } }
    }

    private fun readName(source: Uri): String = query(source, OpenableColumns.DISPLAY_NAME) { cursor, index ->
        cursor.getString(index)
    }
        ?.takeIf { it.isNotBlank() }
        ?.take(MAX_FILE_NAME_LENGTH)
        ?: DEFAULT_FILE_NAME

    private fun readSize(source: Uri): Long? = query(source, OpenableColumns.SIZE) { cursor, index ->
        if (cursor.isNull(index)) null else cursor.getLong(index)
    }

    private fun <T> query(source: Uri, column: String, read: (Cursor, Int) -> T?): T? =
        context.contentResolver.query(source, arrayOf(column), null, null, null)?.use {
            val index = it.getColumnIndex(column)
            if (index >= 0 && it.moveToFirst()) read(it, index) else null
        }

    private fun directory() = context.attachmentDirectory()
}
