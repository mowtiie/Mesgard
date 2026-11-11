package com.mowtiie.mesgard.domain.attachment

internal const val MAX_ATTACHMENT_BYTES = 20L * 1024 * 1024
internal const val MAX_ATTACHMENTS_PER_MESSAGE = 10

/** Must match the allowed MIME types of the `message-attachments` bucket. */
internal val ALLOWED_ATTACHMENT_TYPES = setOf(
    "image/jpeg",
    "image/png",
    "image/webp",
    "image/gif",
    "application/pdf",
    "text/plain",
    "text/csv",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
)

internal sealed interface ImportResult {
    data class Imported(val fileName: String, val mimeType: String, val sizeBytes: Long, val localPath: String) :
        ImportResult

    data class Rejected(val reason: AttachmentRejection) : ImportResult
}

internal enum class AttachmentRejection {
    UNREADABLE,
    TOO_LARGE,
    TYPE_NOT_ALLOWED,
}

internal interface AttachmentImporter {
    /** Copies the picked file into app storage under [attachmentId], shrinking images and removing their metadata. */
    suspend fun import(uri: String, attachmentId: String): ImportResult

    fun delete(localPath: String)
}
