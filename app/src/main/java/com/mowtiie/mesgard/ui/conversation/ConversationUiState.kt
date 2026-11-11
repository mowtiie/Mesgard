package com.mowtiie.mesgard.ui.conversation

import com.mowtiie.mesgard.domain.model.ConversationType
import com.mowtiie.mesgard.domain.model.Message
import com.mowtiie.mesgard.domain.repository.MAX_MESSAGE_LENGTH

internal enum class ConversationError {
    NETWORK_UNAVAILABLE,
    UNKNOWN,
    MESSAGE_TOO_LONG,
    NOT_SIGNED_IN,
    CONVERSATION_UNAVAILABLE,
    TOO_MANY_ATTACHMENTS,
    ATTACHMENT_TOO_LARGE,
    ATTACHMENT_TYPE_NOT_ALLOWED,
    ATTACHMENT_UNREADABLE,
}

internal data class ConversationUiState(
    val title: String? = null,
    val type: ConversationType? = null,
    val canPost: Boolean = true,
    val messages: List<Message> = emptyList(),
    val draft: String = "",
    val attachments: List<PickedAttachment> = emptyList(),
    val typingNames: List<String> = emptyList(),
    val isLoaded: Boolean = false,
    val isLoadingOlder: Boolean = false,
    val error: ConversationError? = null,
) {
    val canSend: Boolean
        get() = (draft.isNotBlank() || attachments.isNotEmpty()) && draft.trim().length <= MAX_MESSAGE_LENGTH

    val showSenderNames: Boolean
        get() = type != null && type != ConversationType.DIRECT

    val showEmptyMessage: Boolean
        get() = isLoaded && messages.isEmpty()
}

/** A file the user chose for the message being written, before it is sent. */
internal data class PickedAttachment(val uri: String, val name: String)
