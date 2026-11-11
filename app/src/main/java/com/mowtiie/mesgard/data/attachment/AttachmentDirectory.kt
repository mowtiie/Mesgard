package com.mowtiie.mesgard.data.attachment

import android.content.Context
import java.io.File

internal const val ATTACHMENT_DIRECTORY = "attachments"

/** Attachment files stay out of backups: the no backup directory is never copied off the device. */
internal fun Context.attachmentDirectory(): File = File(noBackupFilesDir, ATTACHMENT_DIRECTORY).also { it.mkdirs() }
