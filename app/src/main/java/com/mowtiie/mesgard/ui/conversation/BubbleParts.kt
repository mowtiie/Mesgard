package com.mowtiie.mesgard.ui.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontStyle
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.Message

/** The sender and the text are read as one item, and a message of the user says so. */
@Composable
internal fun MessageText(message: Message, showSenderName: Boolean) {
    val sentByYou = stringResource(R.string.message_sent_by_you)
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {
            if (message.isOwn) stateDescription = sentByYou
        },
    ) {
        val name = message.senderName
        if (showSenderName && !message.isOwn && name != null) {
            Text(text = name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        MessageBody(message)
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
    } else if (message.body.isNotEmpty()) {
        Text(text = remember(message.body) { linkedText(message.body) }, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
internal fun FailedMessageNotice(onRetry: () -> Unit, onDiscard: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
        Text(
            text = stringResource(R.string.message_not_sent),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
        )
        TextButton(onClick = onRetry) { Text(stringResource(R.string.message_retry)) }
        TextButton(onClick = onDiscard) { Text(stringResource(R.string.message_discard)) }
    }
}
