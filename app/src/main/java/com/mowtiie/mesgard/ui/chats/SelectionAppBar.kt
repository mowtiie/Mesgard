package com.mowtiie.mesgard.ui.chats

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
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
                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.selection_close))
            }
        },
        actions = {
            if (state.actions.archive == ArchiveAction.ARCHIVE) PinButton(state.actions.pin, callbacks.onPin)
            ArchiveButton(state.actions.archive, callbacks.onArchive)
            IconButton(onClick = callbacks.onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.conversation_delete))
            }
            OverflowMenu { dismiss ->
                MuteItem(state.actions.mute) {
                    dismiss()
                    callbacks.onMute()
                }
                ReadItem(state.actions.read) {
                    dismiss()
                    callbacks.onRead()
                }
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

/** The icon shows the state the conversation will be in: filled once pinned, outlined while it can be pinned. */
@Composable
private fun PinButton(action: PinAction, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        if (action == PinAction.PIN) {
            Icon(painterResource(R.drawable.ic_push_pin_outline), stringResource(R.string.conversation_pin))
        } else {
            Icon(painterResource(R.drawable.ic_push_pin), stringResource(R.string.conversation_unpin))
        }
    }
}

@Composable
private fun ArchiveButton(action: ArchiveAction, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        if (action == ArchiveAction.ARCHIVE) {
            Icon(painterResource(R.drawable.ic_archive_outline), stringResource(R.string.conversation_archive))
        } else {
            Icon(painterResource(R.drawable.ic_unarchive_outline), stringResource(R.string.conversation_unarchive))
        }
    }
}

@Composable
private fun MuteItem(action: MuteAction, onClick: () -> Unit) {
    val label = if (action == MuteAction.MUTE) R.string.conversation_mute else R.string.conversation_unmute
    DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = onClick)
}

@Composable
private fun ReadItem(action: ReadAction, onClick: () -> Unit) {
    val label = if (action ==
        ReadAction.MARK_READ
    ) {
        R.string.conversation_mark_read
    } else {
        R.string.conversation_mark_unread
    }
    DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = onClick)
}
