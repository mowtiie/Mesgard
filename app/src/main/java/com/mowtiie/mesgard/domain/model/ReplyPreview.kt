package com.mowtiie.mesgard.domain.model

/**
 * What a reply quotes. [body] and [senderName] are null while the original is older than the messages on the device,
 * and the text is blank when the original was deleted or only had attachments.
 */
internal data class ReplyPreview(
    val messageId: String,
    val isLoaded: Boolean,
    val senderName: String?,
    val body: String,
    val isDeleted: Boolean,
)
