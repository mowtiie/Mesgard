package com.mowtiie.mesgard.ui.chats

import com.mowtiie.mesgard.domain.model.ConversationSummary

internal enum class MuteAction { MUTE, UNMUTE }

internal enum class ReadAction { MARK_READ, MARK_UNREAD }

internal enum class PinAction { PIN, UNPIN }

internal enum class ArchiveAction { ARCHIVE, UNARCHIVE }

/**
 * What each toggle does to the whole selection. A toggle turns on for everyone unless it is already on for everyone,
 * so a mixed selection first becomes uniform: muted, read, pinned or archived.
 */
internal data class SelectionActions(
    val mute: MuteAction,
    val read: ReadAction,
    val pin: PinAction,
    val archive: ArchiveAction,
)

internal fun selectionActionsFor(selected: List<ConversationSummary>) = SelectionActions(
    mute = if (selected.isNotEmpty() && selected.all { it.isMuted }) MuteAction.UNMUTE else MuteAction.MUTE,
    read = if (selected.any { it.hasUnread }) ReadAction.MARK_READ else ReadAction.MARK_UNREAD,
    pin = if (selected.isNotEmpty() && selected.all { it.isPinned }) PinAction.UNPIN else PinAction.PIN,
    archive = if (selected.isNotEmpty() && selected.all { it.isArchived }) {
        ArchiveAction.UNARCHIVE
    } else {
        ArchiveAction.ARCHIVE
    },
)

internal val ConversationSummary.hasUnread: Boolean
    get() = unreadCount > 0 || isMarkedUnread
