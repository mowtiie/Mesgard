package com.mowtiie.mesgard.ui.chats

import com.mowtiie.mesgard.domain.model.ConversationSummary
import com.mowtiie.mesgard.domain.model.ConversationType
import org.junit.Assert.assertEquals
import org.junit.Test

class SelectionActionsTest {
    private fun conversation(
        id: String = "c",
        unreadCount: Int = 0,
        isMuted: Boolean = false,
        isMarkedUnread: Boolean = false,
    ) = ConversationSummary(
        id = id,
        type = ConversationType.DIRECT,
        title = id,
        lastMessagePreview = null,
        lastActivityAtEpochMillis = 1L,
        unreadCount = unreadCount,
        canPost = true,
        isMuted = isMuted,
        isMarkedUnread = isMarkedUnread,
    )

    @Test
    fun `muting is offered until every selected conversation is muted`() {
        assertEquals(MuteAction.MUTE, selectionActionsFor(listOf(conversation())).mute)
        assertEquals(
            MuteAction.MUTE,
            selectionActionsFor(listOf(conversation("a", isMuted = true), conversation("b"))).mute,
        )
        assertEquals(
            MuteAction.UNMUTE,
            selectionActionsFor(listOf(conversation("a", isMuted = true), conversation("b", isMuted = true))).mute,
        )
    }

    @Test
    fun `marking as read is offered while anything selected is unread`() {
        assertEquals(ReadAction.MARK_UNREAD, selectionActionsFor(listOf(conversation())).read)
        assertEquals(ReadAction.MARK_READ, selectionActionsFor(listOf(conversation(unreadCount = 2))).read)
        assertEquals(ReadAction.MARK_READ, selectionActionsFor(listOf(conversation(isMarkedUnread = true))).read)
        assertEquals(
            ReadAction.MARK_READ,
            selectionActionsFor(listOf(conversation("a"), conversation("b", unreadCount = 1))).read,
        )
    }
}
