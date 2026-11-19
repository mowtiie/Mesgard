package com.mowtiie.mesgard.ui.conversation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.SearchHit
import com.mowtiie.mesgard.ui.common.OverflowMenu
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

/** The actions of the conversation bar: the search button and the menu with mute. */
@Composable
internal fun ConversationBarActions(isMuted: Boolean, onSearch: () -> Unit, onMuteChanged: (Boolean) -> Unit) {
    IconButton(onClick = onSearch) {
        Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.conversation_search))
    }
    OverflowMenu { dismiss ->
        MuteMenuItem(isMuted) {
            dismiss()
            onMuteChanged(!isMuted)
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
