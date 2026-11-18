package com.mowtiie.mesgard.ui.conversation

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mowtiie.mesgard.R

/** Pin or Unpin in the menu of a message, for administrators and staff. */
@Composable
internal fun PinMenuItem(messageId: String, isPinned: Boolean, actions: MessageActions, onDismiss: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(if (isPinned) R.string.message_unpin else R.string.message_pin)) },
        onClick = {
            onDismiss()
            if (isPinned) actions.onUnpin(messageId) else actions.onPin(messageId)
        },
    )
}
