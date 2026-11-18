package com.mowtiie.mesgard.domain.model

/** Someone who has read a message, and when. */
internal data class MessageReader(val userId: String, val displayName: String, val readAtEpochMillis: Long)

internal sealed interface ReadersResult {
    /** At most [com.mowtiie.mesgard.domain.repository.MESSAGE_READERS_LIMIT] readers, the latest first. */
    data class Loaded(val readers: List<MessageReader>) : ReadersResult

    data class Failure(val error: RefreshError) : ReadersResult
}
