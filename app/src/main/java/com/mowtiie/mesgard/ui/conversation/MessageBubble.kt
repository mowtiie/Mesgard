package com.mowtiie.mesgard.ui.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.Message
import com.mowtiie.mesgard.domain.model.MessageStatus
import com.mowtiie.mesgard.ui.connectivity.LocalIsOffline
import com.mowtiie.mesgard.ui.theme.MesgardTheme

private val BubbleMaxWidth = 320.dp
private val BubblePadding = 12.dp
private val RowPadding = 12.dp
private val RowSpacing = 2.dp
private const val METADATA_SEPARATOR = " · "

@Composable
internal fun MessageBubble(
    message: Message,
    showSenderName: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = RowPadding, vertical = RowSpacing),
        horizontalAlignment = if (message.isOwn) Alignment.End else Alignment.Start,
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = if (message.isOwn) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            contentColor = if (message.isOwn) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        ) {
            Column(modifier = Modifier.widthIn(max = BubbleMaxWidth).padding(BubblePadding)) {
                SenderName(message, showSenderName)
                MessageBody(message)
                Text(
                    text = metadata(message),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        if (message.status == MessageStatus.FAILED) FailedMessageNotice(onRetry)
    }
}

@Composable
private fun SenderName(message: Message, showSenderName: Boolean) {
    val name = message.senderName
    if (showSenderName && !message.isOwn && name != null) {
        Text(text = name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun MessageBody(message: Message) {
    if (message.isDeleted) {
        Text(
            text = stringResource(R.string.message_deleted),
            style = MaterialTheme.typography.bodyLarge,
            fontStyle = FontStyle.Italic,
        )
    } else {
        Text(text = message.body, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun metadata(message: Message): String {
    val parts = listOfNotNull(
        formatMessageTime(message.createdAtEpochMillis),
        if (message.isEdited && !message.isDeleted) stringResource(R.string.message_edited) else null,
        if (message.status == MessageStatus.PENDING) pendingLabel() else null,
    )
    return parts.joinToString(METADATA_SEPARATOR)
}

@Composable
private fun FailedMessageNotice(onRetry: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
        Text(
            text = stringResource(R.string.message_not_sent),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
        )
        TextButton(onClick = onRetry) { Text(stringResource(R.string.message_retry)) }
    }
}

@Preview(showBackground = true)
@Composable
private fun MessageBubblePreview() {
    MesgardTheme {
        Column {
            MessageBubble(
                message = previewMessage(isOwn = false, body = "Reminder: bring your project proposals tomorrow."),
                showSenderName = true,
                onRetry = {},
            )
            MessageBubble(
                message = previewMessage(isOwn = true, body = "Noted, thank you.", status = MessageStatus.FAILED),
                showSenderName = true,
                onRetry = {},
            )
        }
    }
}

private fun previewMessage(isOwn: Boolean, body: String, status: MessageStatus = MessageStatus.SENT) = Message(
    id = body,
    conversationId = "chat",
    senderId = "sender",
    senderName = "Staff One",
    body = body,
    createdAtEpochMillis = System.currentTimeMillis(),
    isEdited = false,
    isDeleted = false,
    status = status,
    isOwn = isOwn,
)

@Composable
private fun pendingLabel(): String = if (LocalIsOffline.current) {
    stringResource(R.string.message_waiting_for_network)
} else {
    stringResource(R.string.message_sending)
}
