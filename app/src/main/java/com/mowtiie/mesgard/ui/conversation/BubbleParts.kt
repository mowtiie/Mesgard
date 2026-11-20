package com.mowtiie.mesgard.ui.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.Message
import com.mowtiie.mesgard.ui.common.avatarAccentColor

/**
 * The sender and the text are read as one item, and a message of the user says so. [endRoom] is left free at the end
 * of the last line for the time of the message.
 */
@Composable
internal fun MessageText(message: Message, showSenderName: Boolean, endRoom: Dp = 0.dp) {
    val sentByYou = stringResource(R.string.message_sent_by_you)
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {
            if (message.isOwn) stateDescription = sentByYou
        },
    ) {
        val name = message.senderName
        if (showSenderName && !message.isOwn && name != null) {
            Text(text = name, style = MaterialTheme.typography.labelMedium, color = avatarAccentColor(name))
        }
        MessageBody(message, endRoom)
    }
}

private const val END_ROOM_ID = "endRoom"

@Composable
private fun MessageBody(message: Message, endRoom: Dp) {
    val text = when {
        message.isDeleted -> AnnotatedString(stringResource(R.string.message_deleted))
        message.body.isNotEmpty() -> remember(message.body, message.mentionNames) {
            linkedText(message.body, message.mentionNames)
        }
        else -> return
    }
    val style = MaterialTheme.typography.bodyLarge
    val room = with(LocalDensity.current) { endRoom.toSp() }
    val withRoom = remember(text, endRoom) {
        if (endRoom > 0.dp) {
            buildAnnotatedString {
                append(text)
                appendInlineContent(END_ROOM_ID, " ")
            }
        } else {
            text
        }
    }
    Text(
        text = withRoom,
        style = style,
        fontStyle = if (message.isDeleted) FontStyle.Italic else null,
        inlineContent = mapOf(
            END_ROOM_ID to InlineTextContent(
                Placeholder(room, style.lineHeight, PlaceholderVerticalAlign.TextBottom),
            ) {},
        ),
    )
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
