package com.mowtiie.mesgard.ui.conversation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.SearchHit
import com.mowtiie.mesgard.ui.common.SearchAppBar

/** What the search of a conversation shows and can do. */
internal data class ConversationSearch(
    val state: ConversationSearchUiState = ConversationSearchUiState(),
    val onQueryChanged: (String) -> Unit = {},
    val onLoadMore: () -> Unit = {},
    val onHitSelected: (SearchHit) -> Unit = {},
    val onJumpHandled: () -> Unit = {},
    val onClosed: () -> Unit = {},
    val onJumpTo: (String) -> Unit = {},
)

/**
 * The actions of the conversation bar: search, and mute as a toggle. The bell is outlined while notifications are on
 * and filled with a slash once the conversation is muted, which is the toggled on state. Mute stops push
 * notifications of this conversation for the current user, and unread counts still count.
 */
@Composable
internal fun ConversationBarActions(isMuted: Boolean, onSearch: () -> Unit, onMuteChanged: (Boolean) -> Unit) {
    IconButton(onClick = onSearch) {
        Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.conversation_search))
    }
    IconButton(onClick = { onMuteChanged(!isMuted) }) {
        if (isMuted) {
            Icon(
                painterResource(R.drawable.ic_notifications_off_filled),
                contentDescription = stringResource(R.string.conversation_unmute),
            )
        } else {
            Icon(Icons.Outlined.Notifications, contentDescription = stringResource(R.string.conversation_mute))
        }
    }
}

@Composable
internal fun ConversationSearchBar(search: ConversationSearch, onClose: () -> Unit) {
    SearchAppBar(
        query = search.state.query,
        onQueryChange = search.onQueryChanged,
        onClose = onClose,
        placeholder = stringResource(R.string.conversation_search_placeholder),
    )
}
