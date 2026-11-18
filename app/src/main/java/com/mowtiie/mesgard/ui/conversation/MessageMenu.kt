package com.mowtiie.mesgard.ui.conversation

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.Message

/** What a long press on a message offers: copy and reply for anyone, and edit, info and delete for the sender. */
@Composable
internal fun MessageMenu(
    expanded: Boolean,
    canCopy: Boolean,
    canDelete: Boolean,
    canReply: Boolean,
    message: Message,
    actions: MessageActions,
    onDismiss: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        if (canReply) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.message_reply)) },
                onClick = {
                    onDismiss()
                    actions.onReply(message)
                },
            )
        }
        if (canCopy) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.message_copy)) },
                onClick = {
                    onDismiss()
                    actions.onCopy(message.body)
                },
            )
        }
        if (canDelete) {
            if (message.body.isNotEmpty()) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.message_edit)) },
                    onClick = {
                        onDismiss()
                        actions.onEdit(message)
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.message_info)) },
                onClick = {
                    onDismiss()
                    actions.onInfo(message.id)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.message_delete)) },
                onClick = {
                    onDismiss()
                    actions.onDelete(message.id)
                },
            )
        }
    }
}
