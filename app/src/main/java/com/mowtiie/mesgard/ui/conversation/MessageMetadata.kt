package com.mowtiie.mesgard.ui.conversation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.Message
import com.mowtiie.mesgard.domain.model.MessageStatus
import com.mowtiie.mesgard.domain.network.ConnectionStatus
import com.mowtiie.mesgard.ui.connectivity.LocalConnectionStatus

private const val METADATA_SEPARATOR = " · "

/** The time of a message, with a note when it was edited or is still waiting to be sent. */
@Composable
internal fun metadata(message: Message): String {
    val parts = listOfNotNull(
        formatMessageTime(message.createdAtEpochMillis),
        if (message.isEdited && !message.isDeleted) stringResource(R.string.message_edited) else null,
        if (message.status == MessageStatus.PENDING) pendingLabel() else null,
    )
    return parts.joinToString(METADATA_SEPARATOR)
}

@Composable
private fun pendingLabel(): String = when (LocalConnectionStatus.current) {
    ConnectionStatus.OFFLINE -> stringResource(R.string.message_waiting_for_network)
    ConnectionStatus.SERVER_UNREACHABLE -> stringResource(R.string.message_waiting_for_server)
    ConnectionStatus.ONLINE -> stringResource(R.string.message_sending)
}
