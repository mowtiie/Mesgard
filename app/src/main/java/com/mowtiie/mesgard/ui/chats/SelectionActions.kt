package com.mowtiie.mesgard.ui.chats

import com.mowtiie.mesgard.domain.model.ConversationSummary

internal enum class MuteAction { MUTE, UNMUTE }

internal enum class ReadAction { MARK_READ, MARK_UNREAD }

internal enum class PinAction { PIN, UNPIN }

/**
 * What each toggle does to the whole selection. A toggle turns on for everyone unless it is already on for everyone,
 * so a mixed selection first becomes uniform: muted, read or pinned.
 */
internal data class SelectionActions(val mute: MuteAction, val read: ReadAction, val pin: PinAction)

internal fun selectionActionsFor(selected: List<ConversationSummary>) = SelectionActions(
    mute = if (selected.isNotEmpty() && selected.all { it.isMuted }) MuteAction.UNMUTE else MuteAction.MUTE,
    read = if (selected.any { it.hasUnread }) ReadAction.MARK_READ else ReadAction.MARK_UNREAD,
    pin = if (selected.isNotEmpty() && selected.all { it.isPinned }) PinAction.UNPIN else PinAction.PIN,
)

internal val ConversationSummary.hasUnread: Boolean
    get() = unreadCount > 0 || isMarkedUnread
