package com.mowtiie.mesgard.data.attachment

import com.mowtiie.mesgard.data.local.AttachmentEntity
import com.mowtiie.mesgard.domain.attachment.AttachmentImporter
import com.mowtiie.mesgard.domain.attachment.AttachmentRejection
import com.mowtiie.mesgard.domain.attachment.ImportResult
import com.mowtiie.mesgard.domain.model.SendRejection
import java.util.UUID
import javax.inject.Inject

internal sealed interface IntakeResult {
    data class Ready(val attachments: List<AttachmentEntity>) : IntakeResult

    data class Rejected(val reason: SendRejection) : IntakeResult
}

/** Imports the files picked for a message. When one is refused, the files already imported are removed. */
internal class AttachmentIntake @Inject constructor(private val importer: AttachmentImporter) {
    suspend fun prepare(conversationId: String, messageId: String, uris: List<String>): IntakeResult {
        val imported = mutableListOf<AttachmentEntity>()
        for (uri in uris) {
            val attachmentId = UUID.randomUUID().toString()
            when (val result = importer.import(uri, attachmentId)) {
                is ImportResult.Imported -> imported += AttachmentEntity(
                    id = attachmentId,
                    messageId = messageId,
                    storagePath = "$conversationId/$messageId/$attachmentId",
                    mimeType = result.mimeType,
                    sizeBytes = result.sizeBytes,
                    fileName = result.fileName,
                    localPath = result.localPath,
                )
                is ImportResult.Rejected -> {
                    imported.mapNotNull { it.localPath }.forEach(importer::delete)
                    return IntakeResult.Rejected(result.reason.toSendRejection())
                }
            }
        }
        return IntakeResult.Ready(imported)
    }
}

private fun AttachmentRejection.toSendRejection() = when (this) {
    AttachmentRejection.UNREADABLE -> SendRejection.ATTACHMENT_UNREADABLE
    AttachmentRejection.TOO_LARGE -> SendRejection.ATTACHMENT_TOO_LARGE
    AttachmentRejection.TYPE_NOT_ALLOWED -> SendRejection.ATTACHMENT_TYPE_NOT_ALLOWED
}
