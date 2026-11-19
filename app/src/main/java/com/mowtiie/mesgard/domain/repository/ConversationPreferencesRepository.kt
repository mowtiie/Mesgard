package com.mowtiie.mesgard.domain.repository

import com.mowtiie.mesgard.domain.model.PreferenceResult

/**
 * What a person does with their own conversation list. Every call takes the whole selection and applies at once on the
 * phone, then on the server; when the server refuses, the earlier state comes back. Nobody else sees any of it.
 */
internal interface ConversationPreferencesRepository {
    /** At most [com.mowtiie.mesgard.domain.model.MAX_PINNED_CONVERSATIONS] stay pinned. */
    suspend fun setPinned(conversationIds: Set<String>, pinned: Boolean): PreferenceResult

    suspend fun setArchived(conversationIds: Set<String>, archived: Boolean): PreferenceResult

    suspend fun setMuted(conversationIds: Set<String>, muted: Boolean): PreferenceResult

    suspend fun markRead(conversationIds: Set<String>): PreferenceResult

    suspend fun markUnread(conversationIds: Set<String>): PreferenceResult

    /** Removes the conversations and clears their history for this person only. New messages bring them back. */
    suspend fun deleteForMe(conversationIds: Set<String>): PreferenceResult

    /** Shows the history again. Pins, the archive and the unread mark that the delete cleared are not restored. */
    suspend fun undoDeleteForMe(conversationIds: Set<String>): PreferenceResult
}
