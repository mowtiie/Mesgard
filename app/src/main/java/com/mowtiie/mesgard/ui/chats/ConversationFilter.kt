package com.mowtiie.mesgard.ui.chats

import androidx.annotation.StringRes
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.ConversationSummary
import com.mowtiie.mesgard.domain.model.ConversationType

/** The filter chips above the conversation list. One is chosen at a time, and All shows everything. */
internal enum class ConversationFilter(
    @StringRes val label: Int,
    @StringRes val emptyTitle: Int,
    @StringRes val emptyHint: Int,
) {
    ALL(R.string.chats_filter_all, R.string.conversation_list_empty, R.string.conversation_list_empty_hint),
    UNREAD(R.string.chats_filter_unread, R.string.chats_filter_unread_empty, R.string.chats_filter_unread_empty_hint),
    FAVORITES(
        R.string.chats_filter_favorites,
        R.string.chats_filter_favorites_empty,
        R.string.chats_filter_favorites_empty_hint,
    ),
    GROUPS(R.string.chats_filter_groups, R.string.chats_filter_groups_empty, R.string.chats_filter_groups_empty_hint),
    ;

    fun accepts(conversation: ConversationSummary): Boolean = when (this) {
        ALL -> true
        UNREAD -> conversation.unreadCount > 0 || conversation.isMarkedUnread
        FAVORITES -> conversation.isWithFavorite
        GROUPS -> conversation.type == ConversationType.GROUP
    }
}
