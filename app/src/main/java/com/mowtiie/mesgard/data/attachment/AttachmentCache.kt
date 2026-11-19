package com.mowtiie.mesgard.data.attachment

import com.mowtiie.mesgard.data.local.AttachmentDao
import java.io.File
import javax.inject.Inject

internal const val ATTACHMENT_CACHE_LIMIT_BYTES = 200L * 1024 * 1024

/**
 * Downloaded files are a cache, because the server keeps every attachment. When they pass the limit, the oldest
 * ones are removed and downloaded again when someone opens them. Files of messages that are not sent yet are never
 * removed.
 */
internal class AttachmentCache @Inject constructor(private val attachmentDao: AttachmentDao) {
    suspend fun trim(keepAttachmentId: String, limitBytes: Long = ATTACHMENT_CACHE_LIMIT_BYTES) {
        val stored = attachmentDao.findStoredOldestFirst()
        var excessBytes = stored.sumOf { it.sizeBytes } - limitBytes
        stored.asSequence()
            .filter { it.id != keepAttachmentId }
            .takeWhile { excessBytes > 0 }
            .forEach { attachment ->
                attachment.localPath?.let { File(it).delete() }
                attachmentDao.clearLocalPath(attachment.id)
                excessBytes -= attachment.sizeBytes
            }
    }
}
