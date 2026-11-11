package com.mowtiie.mesgard.data.work

import com.mowtiie.mesgard.data.local.AttachmentDao
import com.mowtiie.mesgard.data.local.ConversationDao
import com.mowtiie.mesgard.data.local.MessageEntity
import com.mowtiie.mesgard.data.remote.AttachmentRemoteSource
import com.mowtiie.mesgard.data.remote.MessageRemoteSource
import com.mowtiie.mesgard.data.remote.parseEpochMillis
import com.mowtiie.mesgard.data.remote.toNewMessageDto
import java.io.File
import java.io.FileNotFoundException
import javax.inject.Inject

/** Sends a stored message to the server and returns the server timestamp. Failures are left to the caller. */
internal class MessageDeliverer @Inject constructor(
    private val conversationDao: ConversationDao,
    private val attachmentDao: AttachmentDao,
    private val messageSource: MessageRemoteSource,
    private val attachmentSource: AttachmentRemoteSource,
) {
    suspend fun deliver(message: MessageEntity): Long {
        val attachments = attachmentDao.findByMessage(message.id)
        if (attachments.isEmpty()) return deliverText(message)
        attachments.forEach { attachment ->
            val file = attachment.localPath?.let(::File)?.takeIf { it.isFile }
                ?: throw FileNotFoundException("The file of attachment ${attachment.id} is missing")
            attachmentSource.upload(attachment.storagePath, file, attachment.mimeType)
        }
        return parseEpochMillis(attachmentSource.sendWithAttachments(message, attachments))
    }

    private suspend fun deliverText(message: MessageEntity): Long {
        val orgId = requireNotNull(conversationDao.findOrgId(message.conversationId)) {
            "Conversation ${message.conversationId} is not stored"
        }
        return parseEpochMillis(messageSource.send(message.toNewMessageDto(orgId)).createdAt)
    }
}
