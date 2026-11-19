package com.mowtiie.mesgard.domain.repository

import kotlinx.coroutines.flow.Flow

internal interface TypingRepository {
    /**
     * Emits the display names of the other participants who are typing in the conversation. Every item of
     * [ownTyping] tells the other participants that the signed-in user is typing (sent at most once per throttle
     * interval). The connection lasts as long as the flow is collected.
     */
    fun observeTypingNames(conversationId: String, ownTyping: Flow<Unit>): Flow<List<String>>

    /**
     * Emits who is typing in each of the conversations, leaving out the ones where nobody is. It only listens, and the
     * connections last as long as the flow is collected, so the caller passes a few conversations at a time.
     */
    fun observeTypingIn(conversationIds: Set<String>): Flow<Map<String, List<String>>>
}
