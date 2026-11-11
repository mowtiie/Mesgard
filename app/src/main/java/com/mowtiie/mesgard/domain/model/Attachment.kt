package com.mowtiie.mesgard.domain.model

internal data class Attachment(
    val id: String,
    val messageId: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val storagePath: String,
    val localPath: String?,
) {
    val isImage: Boolean get() = mimeType.startsWith("image/")
}
