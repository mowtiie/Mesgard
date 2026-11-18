package com.mowtiie.mesgard.domain.push

internal interface ConversationNotifications {
    /** Removes the notification of a conversation the person is now reading. */
    fun clear(conversationId: String)
}
