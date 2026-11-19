package com.mowtiie.mesgard.ui.chats

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.mowtiie.mesgard.R

/** The main action of Chats. The label folds away while the list is scrolled, leaving the icon. */
@Composable
internal fun NewChatButton(listState: LazyListState, onClick: () -> Unit) {
    val isExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    ExtendedFloatingActionButton(
        onClick = onClick,
        expanded = isExpanded,
        icon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
        text = { Text(stringResource(R.string.new_chat_title)) },
    )
}
