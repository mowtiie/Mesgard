package com.mowtiie.mesgard.domain.model

internal data class Attachment(
    val id: String,
    val messageId: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val storagePath: String,
    val localPath: String?,
    val width: Int? = null,
    val height: Int? = null,
) {
    val isImage: Boolean get() = mimeType.startsWith("image/")

    /** Width divided by height, or null when the pixel size is not known. */
    val aspectRatio: Float?
        get() = if (width != null && height != null) width.toFloat() / height else null
}
