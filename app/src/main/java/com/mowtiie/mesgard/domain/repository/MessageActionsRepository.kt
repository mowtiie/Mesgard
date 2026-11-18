package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.RefreshResult

/** What a person can do with a message they sent. Every call needs a connection to the server. */
internal interface MessageActionsRepository {
    /** Deletes the message for everyone. Their devices keep a note that it was deleted. */
    suspend fun delete(messageId: String): RefreshResult
}
