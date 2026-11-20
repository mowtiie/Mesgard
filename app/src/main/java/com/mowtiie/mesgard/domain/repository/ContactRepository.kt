package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.Contact
import com.mowtiie.mesgard.domain.model.ContactSearchResult
import com.mowtiie.mesgard.domain.model.PreferenceResult
import com.mowtiie.mesgard.domain.model.RefreshResult
import com.mowtiie.mesgard.domain.model.StartConversationResult
import kotlinx.coroutines.flow.Flow

/** The most people the roster holds, so a count that reaches it may be higher in reality. */
internal const val ROSTER_LIMIT = 200

internal interface ContactRepository {
    fun observeContacts(): Flow<List<Contact>>

    /** The people of the newest direct conversations, for the list shown before anything is typed. */
    fun observeRecentContacts(limit: Int): Flow<List<Contact>>

    /** Fetches the whole roster and the favorites. */
    suspend fun refresh(): RefreshResult

    /**
     * For opening a screen: the favorites are always fetched, and the roster only when it was not fetched in the last
     * few minutes, because the roster changes rarely and is not what the screen lists.
     */
    suspend fun refreshIfStale(): RefreshResult

    suspend fun startDirectConversation(contact: Contact): StartConversationResult

    /** Looks for people by name on the server, so an organization larger than the stored roster is searchable. */
    suspend fun search(query: String): ContactSearchResult

    /** Adds or removes favorites on the phone first, then on the server, and puts them back when it refuses. */
    suspend fun setFavorite(contacts: Set<Contact>, favorite: Boolean): PreferenceResult
}
