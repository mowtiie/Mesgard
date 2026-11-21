package com.mowtiie.mesgard.ui.conversation

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.Message
import com.mowtiie.mesgard.ui.common.OverflowMenu

/**
 * The action bar of selected messages. It replaces the top app bar like the one of the conversation list: the close
 * button and Back leave the selection, and reply, copy and delete are icons. Edit, info and pin are in the menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MessageSelectionBar(
    selected: List<Message>,
    actions: MessageSelectionActions,
    callbacks: MessageSelectionCallbacks,
    onCopy: (String) -> Unit,
) {
    BackHandler(onBack = callbacks.onClear)
    val single = selected.singleOrNull()
    val done = { callbacks.onClear() }
    TopAppBar(
        title = { Text(pluralStringResource(R.plurals.selection_count, selected.size, selected.size)) },
        navigationIcon = {
            IconButton(onClick = callbacks.onClear) {
                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.selection_close))
            }
        },
        actions = {
            if (actions.canReply && single != null) {
                IconButton(
                    onClick = {
                        callbacks.onReply(single)
                        done()
                    },
                ) {
                    Icon(
                        painterResource(R.drawable.ic_reply),
                        contentDescription = stringResource(R.string.message_reply),
                    )
                }
            }
            if (actions.canCopy) {
                IconButton(
                    onClick = {
                        onCopy(copyTextOf(selected))
                        done()
                    },
                ) {
                    Icon(
                        painterResource(R.drawable.ic_content_copy),
                        contentDescription = stringResource(R.string.message_copy),
                    )
                }
            }
            if (actions.canDelete) {
                IconButton(
                    onClick = {
                        callbacks.onDelete(selected.map { it.id })
                        done()
                    },
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.message_delete))
                }
            }
            if (single != null && hasMenuActions(actions)) {
                OverflowMenu { dismiss ->
                    MenuActions(single, actions, callbacks) {
                        dismiss()
                        done()
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    )
}

private fun hasMenuActions(actions: MessageSelectionActions) =
    actions.canEdit || actions.canShowInfo || actions.canReport || actions.pin != PinChoice.NONE

@Composable
private fun MenuActions(
    message: Message,
    actions: MessageSelectionActions,
    callbacks: MessageSelectionCallbacks,
    onChosen: () -> Unit,
) {
    // An item that does not apply to this message stays in the menu, disabled, as the Material menu guidelines ask.
    DropdownMenuItem(
        text = { Text(stringResource(R.string.message_edit)) },
        enabled = actions.canEdit,
        onClick = {
            onChosen()
            callbacks.onEdit(message)
        },
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.message_info)) },
        enabled = actions.canShowInfo,
        onClick = {
            onChosen()
            callbacks.onInfo(message.id)
        },
    )
    if (actions.canManagePins) {
        val isUnpin = actions.pin == PinChoice.UNPIN
        DropdownMenuItem(
            text = { Text(stringResource(if (isUnpin) R.string.message_unpin else R.string.message_pin)) },
            enabled = actions.pin != PinChoice.NONE,
            onClick = {
                onChosen()
                if (isUnpin) callbacks.onUnpin(message.id) else callbacks.onPin(message.id)
            },
        )
    }
    // Reporting is not a message edit, so it is set apart at the end of the menu.
    HorizontalDivider()
    DropdownMenuItem(
        text = { Text(stringResource(R.string.message_report)) },
        enabled = actions.canReport,
        onClick = {
            onChosen()
            callbacks.onReport(message.id)
        },
    )
}
