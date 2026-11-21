package com.mowtiie.mesgard.data.work

import com.mowtiie.mesgard.data.attachment.AttachmentVault
import com.mowtiie.mesgard.data.local.AttachmentDao
import com.mowtiie.mesgard.data.local.AttachmentEntity
import com.mowtiie.mesgard.data.local.ConversationDao
import com.mowtiie.mesgard.data.local.MessageEntity
import com.mowtiie.mesgard.data.remote.AttachmentRemoteSource
import com.mowtiie.mesgard.data.remote.MessageRemoteSource
import com.mowtiie.mesgard.data.remote.parseEpochMillis
import com.mowtiie.mesgard.data.remote.toNewMessageDto
import com.mowtiie.mesgard.domain.attachment.AttachmentImporter
import com.mowtiie.mesgard.domain.attachment.UNOPTIMIZED_SUFFIX
import java.io.File
import java.io.FileNotFoundException
import javax.inject.Inject

/** Sends a stored message to the server and returns the server timestamp. Failures are left to the caller. */
internal class MessageDeliverer @Inject constructor(
    private val conversationDao: ConversationDao,
    private val attachmentDao: AttachmentDao,
    private val messageSource: MessageRemoteSource,
    private val attachmentSource: AttachmentRemoteSource,
    private val importer: AttachmentImporter,
    private val vault: AttachmentVault,
) {
    suspend fun deliver(message: MessageEntity): Long {
        val attachments = attachmentDao.findByMessage(message.id).map { shrinkIfPhoto(it) }
        if (attachments.isEmpty()) return deliverText(message)
        attachments.forEach { attachment ->
            val file = attachment.localPath?.let(::File)?.takeIf { it.isFile }
                ?: throw FileNotFoundException("The file of attachment ${attachment.id} is missing")
            vault.withPlainFile(file) { plain ->
                attachmentSource.upload(attachment.storagePath, plain, attachment.mimeType)
            }
        }
        val sentAt = parseEpochMillis(attachmentSource.sendWithAttachments(message, attachments))
        attachments.forEach(::sealQuietly)
        return sentAt
    }

    /** A sent file is encrypted on the phone. When that fails the file stays and the next start tries again. */
    private fun sealQuietly(attachment: AttachmentEntity) {
        attachment.localPath?.let { runCatching { vault.seal(File(it)) } }
    }

    /** A photo is shrunk here, off the screen, so the message appears as soon as it is sent. */
    private suspend fun shrinkIfPhoto(attachment: AttachmentEntity): AttachmentEntity {
        val rawPath = attachment.localPath?.takeIf { it.endsWith(UNOPTIMIZED_SUFFIX) } ?: return attachment
        val shrunk = importer.optimize(attachment.id, rawPath, attachment.mimeType)
        attachmentDao.updateFile(attachment.id, shrunk.localPath, shrunk.sizeBytes, shrunk.width, shrunk.height)
        importer.delete(rawPath)
        return attachment.copy(
            localPath = shrunk.localPath,
            sizeBytes = shrunk.sizeBytes,
            width = shrunk.width,
            height = shrunk.height,
        )
    }

    private suspend fun deliverText(message: MessageEntity): Long {
        val orgId = requireNotNull(conversationDao.findOrgId(message.conversationId)) {
            "Conversation ${message.conversationId} is not stored"
        }
        return parseEpochMillis(messageSource.send(message.toNewMessageDto(orgId)).createdAt)
    }
}
