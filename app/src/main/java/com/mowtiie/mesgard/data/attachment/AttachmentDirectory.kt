package com.mowtiie.mesgard.data.attachment

import android.content.Context
import java.io.File

internal const val ATTACHMENT_DIRECTORY = "attachments"

/** Attachment files stay out of backups: the no backup directory is never copied off the device. */
internal fun Context.attachmentDirectory(): File = File(noBackupFilesDir, ATTACHMENT_DIRECTORY).also { it.mkdirs() }

internal const val SHARED_ATTACHMENT_DIRECTORY = "shared_attachments"

/** Copies a stored attachment into the cache so another app can read it through the FileProvider. */
internal fun Context.copyForSharing(attachmentId: String, fileName: String, source: File): File {
    val directory = File(cacheDir, SHARED_ATTACHMENT_DIRECTORY).also { it.mkdirs() }
    val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
    return File(directory, "$attachmentId-$safeName").also { source.copyTo(it, overwrite = true) }
}
