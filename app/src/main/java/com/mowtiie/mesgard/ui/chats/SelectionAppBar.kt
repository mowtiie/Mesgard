package com.mowtiie.mesgard.ui.chats

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.ui.common.OverflowMenu

/**
 * The contextual action bar of a list selection: it replaces the top app bar while something is selected, shows how
 * many are selected, and holds the actions. Back and the close button leave the selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectionAppBar(
    state: ConversationSelectionUiState,
    callbacks: SelectionCallbacks,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = callbacks.onClose)
    val count = state.selected.size
    TopAppBar(
        modifier = modifier,
        title = { Text(pluralStringResource(R.plurals.selection_count, count, count)) },
        navigationIcon = {
            IconButton(onClick = callbacks.onClose) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.selection_close))
            }
        },
        actions = {
            MuteButton(state.actions.mute, callbacks.onMute)
            ReadButton(state.actions.read, callbacks.onRead)
            OverflowMenu { dismiss ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.selection_select_all)) },
                    onClick = {
                        dismiss()
                        callbacks.onSelectAll()
                    },
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    )
}

@Composable
private fun MuteButton(action: MuteAction, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        if (action == MuteAction.MUTE) {
            Icon(painterResource(R.drawable.ic_notifications_off), stringResource(R.string.conversation_mute))
        } else {
            Icon(Icons.Default.Notifications, stringResource(R.string.conversation_unmute))
        }
    }
}

@Composable
private fun ReadButton(action: ReadAction, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        if (action == ReadAction.MARK_READ) {
            Icon(Icons.Default.Done, stringResource(R.string.conversation_mark_read))
        } else {
            Icon(Icons.Default.Email, stringResource(R.string.conversation_mark_unread))
        }
    }
}
