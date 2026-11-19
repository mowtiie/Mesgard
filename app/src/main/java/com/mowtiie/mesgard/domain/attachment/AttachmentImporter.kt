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

/** A photo copied from the picker keeps this ending until it is shrunk, which happens when it is sent. */
internal const val UNOPTIMIZED_SUFFIX = ".raw"

/** A shrunk photo without metadata, ready to upload. */
internal data class OptimizedFile(val localPath: String, val sizeBytes: Long, val width: Int?, val height: Int?)

internal sealed interface ImportResult {
    data class Imported(
        val fileName: String,
        val mimeType: String,
        val sizeBytes: Long,
        val localPath: String,
        val width: Int? = null,
        val height: Int? = null,
    ) : ImportResult

    data class Rejected(val reason: AttachmentRejection) : ImportResult
}

internal enum class AttachmentRejection {
    UNREADABLE,
    TOO_LARGE,
    TYPE_NOT_ALLOWED,
}

internal interface AttachmentImporter {
    /**
     * Copies the picked file into app storage under [attachmentId]. A photo is copied as it is, so the message
     * can appear at once, and [optimize] shrinks it later.
     */
    suspend fun import(uri: String, attachmentId: String): ImportResult

    /**
     * Shrinks the photo copied by [import] and removes its metadata, so the location and the camera details
     * never leave the device. A photo that cannot be decoded throws [IllegalArgumentException].
     */
    suspend fun optimize(attachmentId: String, rawPath: String, mimeType: String): OptimizedFile

    fun delete(localPath: String)
}
