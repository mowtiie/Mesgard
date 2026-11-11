package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.DownloadResult

internal interface AttachmentRepository {
    /** Makes sure the file of an attachment is stored on the device, downloading it when it is not. */
    suspend fun download(attachmentId: String): DownloadResult
}
