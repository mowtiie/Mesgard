package com.mowtiie.mesgard.data.remote

import com.mowtiie.mesgard.data.local.AttachmentEntity
import com.mowtiie.mesgard.domain.model.Attachment
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AttachmentDto(
    val id: String,
    @SerialName("storage_path") val storagePath: String,
    @SerialName("mime_type") val mimeType: String,
    @SerialName("size_bytes") val sizeBytes: Long,
    @SerialName("file_name") val fileName: String,
    @SerialName("message_id") val messageId: String? = null,
)

internal fun AttachmentDto.toEntity(parentMessageId: String, localPath: String? = null) = AttachmentEntity(
    id = id,
    messageId = messageId ?: parentMessageId,
    storagePath = storagePath,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    fileName = fileName,
    localPath = localPath,
)

internal fun AttachmentEntity.toDomain() = Attachment(
    id = id,
    messageId = messageId,
    fileName = fileName,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    storagePath = storagePath,
    localPath = localPath,
)
