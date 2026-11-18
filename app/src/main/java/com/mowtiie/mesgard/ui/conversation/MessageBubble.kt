package com.mowtiie.mesgard.ui.conversation

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.Attachment
import com.mowtiie.mesgard.domain.model.Message
import com.mowtiie.mesgard.domain.model.MessageStatus
import com.mowtiie.mesgard.ui.theme.MesgardTheme

private val BubbleMaxWidth = 480.dp
private const val BUBBLE_WIDTH_FRACTION = 0.85f
private val BubblePadding = 12.dp
private val RowPadding = 12.dp
private val RowSpacing = 2.dp
private val GroupSpacing = 8.dp

/** What the user can do with a message or one of its attachments. */
internal data class MessageActions(
    val onRetry: (String) -> Unit = {},
    val onDiscard: (String) -> Unit = {},
    val onCopy: (String) -> Unit = {},
    val onDelete: (String) -> Unit = {},
    val onInfo: (String) -> Unit = {},
    val onEdit: (Message) -> Unit = {},
    val onReply: (Message) -> Unit = {},
    val onReplyClick: (String) -> Unit = {},
    val onAttachmentClick: (Attachment) -> Unit = {},
    val onImageClick: (Attachment) -> Unit = {},
    val onImageShown: (Attachment) -> Unit = {},
    val onRetryDownload: (Attachment) -> Unit = {},
)

@Composable
internal fun MessageBubble(
    message: Message,
    showSenderName: Boolean,
    downloads: AttachmentDownloadsUiState,
    actions: MessageActions,
    modifier: Modifier = Modifier,
    startsGroup: Boolean = true,
    endsGroup: Boolean = true,
) {
    val canReply = !message.isDeleted && message.status == MessageStatus.SENT
    ReplySwipe(enabled = canReply, onReply = { actions.onReply(message) }) {
        BubbleBody(message, showSenderName, downloads, actions, modifier, startsGroup, endsGroup, canReply)
    }
}

@Composable
private fun BubbleBody(
    message: Message,
    showSenderName: Boolean,
    downloads: AttachmentDownloadsUiState,
    actions: MessageActions,
    modifier: Modifier,
    startsGroup: Boolean,
    endsGroup: Boolean,
    canReply: Boolean,
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    val colors = bubbleColors(message)
    val canCopy = !message.isDeleted && message.body.isNotEmpty()
    val canDelete = message.isOwn && !message.isDeleted && message.status == MessageStatus.SENT
    val copyLabel = stringResource(R.string.message_copy)
    val haptics = LocalHapticFeedback.current
    Column(
        modifier = modifier.fillMaxWidth().padding(
            start = RowPadding,
            end = RowPadding,
            top = if (startsGroup) GroupSpacing else RowSpacing,
            bottom = RowSpacing,
        ),
        horizontalAlignment = if (message.isOwn) Alignment.End else Alignment.Start,
    ) {
        Box {
            Surface(
                modifier = Modifier.onLongPress(canCopy || canDelete || canReply, copyLabel) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    isMenuOpen = true
                },
                shape = MaterialTheme.shapes.large,
                color = colors.container,
                contentColor = colors.content,
            ) {
                BubbleContent(message, showSenderName && startsGroup, endsGroup, downloads, actions)
            }
            MessageMenu(isMenuOpen, canCopy, canDelete, canReply, message, actions) { isMenuOpen = false }
        }
        if (message.status == MessageStatus.FAILED) {
            FailedMessageNotice({ actions.onRetry(message.id) }, { actions.onDiscard(message.id) })
        }
    }
}

@Composable
private fun BubbleContent(
    message: Message,
    showSenderName: Boolean,
    endsGroup: Boolean,
    downloads: AttachmentDownloadsUiState,
    actions: MessageActions,
) {
    Column(modifier = Modifier.widthIn(max = maxBubbleWidth()).padding(BubblePadding)) {
        message.replyTo?.let { QuotedReply(it, onClick = { id -> actions.onReplyClick(id) }) }
        MessageText(message, showSenderName)
        if (!message.isDeleted) {
            AttachmentList(
                message.attachments,
                downloads,
                actions.onAttachmentClick,
                actions.onImageClick,
                actions.onImageShown,
                actions.onRetryDownload,
            )
        }
        if (endsGroup || message.status != MessageStatus.SENT || message.isEdited) {
            Text(text = metadata(message), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun maxBubbleWidth() = (LocalConfiguration.current.screenWidthDp * BUBBLE_WIDTH_FRACTION).dp
    .coerceAtMost(BubbleMaxWidth)

/** A long press runs [action] and is offered to TalkBack as a custom action, so it needs no gesture. */
private fun Modifier.onLongPress(enabled: Boolean, label: String, action: () -> Unit): Modifier = if (enabled) {
    pointerInput(action) { detectTapGestures(onLongPress = { action() }) }
        .semantics {
            onLongClick(label) {
                action()
                true
            }
        }
} else {
    this
}

private data class BubbleColors(val container: Color, val content: Color)

@Composable
private fun bubbleColors(message: Message): BubbleColors {
    val scheme = MaterialTheme.colorScheme
    return when {
        message.status == MessageStatus.FAILED -> BubbleColors(scheme.errorContainer, scheme.onErrorContainer)
        message.isOwn -> BubbleColors(scheme.primaryContainer, scheme.onPrimaryContainer)
        else -> BubbleColors(scheme.surfaceContainerHigh, scheme.onSurface)
    }
}

@Preview(showBackground = true)
@Composable
private fun MessageBubblePreview() {
    MesgardTheme {
        Column {
            MessageBubble(
                message = previewMessage(isOwn = false, body = "Reminder: see https://example.com/week-12 tomorrow."),
                showSenderName = true,
                downloads = AttachmentDownloadsUiState(),
                actions = MessageActions(),
            )
            MessageBubble(
                message = previewMessage(isOwn = true, body = "Noted, thank you.", status = MessageStatus.FAILED),
                showSenderName = true,
                downloads = AttachmentDownloadsUiState(),
                actions = MessageActions(),
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
