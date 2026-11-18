package com.mowtiie.mesgard.domain.model

/** A message that administrators or staff pinned to the top of a conversation. */
internal data class PinnedMessage(val messageId: String, val senderName: String, val body: String)

internal sealed interface PinsOutcome {
    data class Loaded(val pins: List<PinnedMessage>) : PinsOutcome

    data class Failure(val error: RefreshError) : PinsOutcome
}
