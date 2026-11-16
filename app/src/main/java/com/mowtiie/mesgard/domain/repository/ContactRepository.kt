package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.model.StartConversationResult
import kotlinx.coroutines.flow.Flow

/** The most people the roster holds, so a count that reaches it may be higher in reality. */
internal const val ROSTER_LIMIT = 200

internal interface ContactRepository {
    fun observeContacts(): Flow<List<Contact>>

    suspend fun refresh(): RefreshResult

    suspend fun startDirectConversation(contact: Contact): StartConversationResult
}
