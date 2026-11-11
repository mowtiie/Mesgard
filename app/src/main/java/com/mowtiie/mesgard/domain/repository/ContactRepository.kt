package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.model.StartConversationResult
import kotlinx.coroutines.flow.Flow

internal interface ContactRepository {
    fun observeContacts(): Flow<List<Contact>>

    suspend fun refresh(): RefreshResult

    suspend fun startDirectConversation(contact: Contact): StartConversationResult
}
