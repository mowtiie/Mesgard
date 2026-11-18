package com.mowtiie.mesgard.ui.conversation

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mowtiie.mesgard.R

/** Mute stops push notifications of this conversation for the current user. Unread counts still count. */
@Composable
internal fun MuteMenuItem(isMuted: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(if (isMuted) R.string.conversation_unmute else R.string.conversation_mute)) },
        onClick = onClick,
    )
}
