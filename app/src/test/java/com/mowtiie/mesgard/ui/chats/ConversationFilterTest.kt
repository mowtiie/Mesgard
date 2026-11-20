package com.mowtiie.mesgard.ui.chats

import com.mowtiie.mesgard.domain.model.ConversationSummary
import com.mowtiie.mesgard.domain.model.ConversationType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationFilterTest {
    private fun conversation(
        type: ConversationType = ConversationType.DIRECT,
        unreadCount: Int = 0,
        isMarkedUnread: Boolean = false,
        isWithFavorite: Boolean = false,
    ) = ConversationSummary(
        id = "c",
        type = type,
        title = "Title",
        lastMessagePreview = null,
        lastActivityAtEpochMillis = 0L,
        unreadCount = unreadCount,
        canPost = true,
        isMarkedUnread = isMarkedUnread,
        isWithFavorite = isWithFavorite,
    )

    @Test
    fun `All accepts every conversation`() {
        assertTrue(ConversationFilter.ALL.accepts(conversation()))
    }

    @Test
    fun `Unread accepts unread messages and a conversation marked as unread`() {
        assertTrue(ConversationFilter.UNREAD.accepts(conversation(unreadCount = 1)))
        assertTrue(ConversationFilter.UNREAD.accepts(conversation(isMarkedUnread = true)))
        assertFalse(ConversationFilter.UNREAD.accepts(conversation()))
    }

    @Test
    fun `Favorites accepts only conversations with a favorite person`() {
        assertTrue(ConversationFilter.FAVORITES.accepts(conversation(isWithFavorite = true)))
        assertFalse(ConversationFilter.FAVORITES.accepts(conversation()))
    }

    @Test
    fun `Groups accepts only group conversations`() {
        assertTrue(ConversationFilter.GROUPS.accepts(conversation(type = ConversationType.GROUP)))
        assertFalse(ConversationFilter.GROUPS.accepts(conversation()))
    }
}
