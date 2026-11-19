package com.mowtiie.mesgard.domain.model

/** A message that administrators or staff pinned to the top of a conversation. */
internal data class PinnedMessage(val messageId: String, val senderName: String, val body: String)
