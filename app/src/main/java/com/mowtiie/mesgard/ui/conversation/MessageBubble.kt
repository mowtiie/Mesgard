package com.mowtiie.mesgard.ui.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.Attachment
import com.mowtiie.mesgard.domain.model.Message
import com.mowtiie.mesgard.domain.model.MessageStatus
import com.mowtiie.mesgard.ui.common.InitialAvatar
import com.mowtiie.mesgard.ui.theme.MesgardTheme

private val BubbleMaxWidth = 480.dp
private const val BUBBLE_WIDTH_FRACTION = 0.85f
private val BubblePadding = 12.dp
private val RowPadding = 12.dp
private val RowSpacing = 2.dp
private val GroupSpacing = 8.dp
private const val SELECTED_ROW_ALPHA = 0.16f
private val AvatarSize = 32.dp
private val AvatarGap = 8.dp
private val AvatarSlotWidth = AvatarSize + AvatarGap
private val LargeEmojiStyle = TextStyle(fontSize = 44.sp, lineHeight = 52.sp)

/** What the user can do with a message or one of its attachments. */
@Immutable
internal data class MessageActions(
    val onRetry: (String) -> Unit = {},
    val onDiscard: (String) -> Unit = {},
    val onReply: (Message) -> Unit = {},
    val onReplyClick: (String) -> Unit = {},
    val onAttachmentClick: (Attachment) -> Unit = {},
    val onImageClick: (Attachment) -> Unit = {},
    val onImageShown: (Attachment) -> Unit = {},
    val onRetryDownload: (Attachment) -> Unit = {},
)

/** Whether messages are being selected, whether this one is, and what a tap or a long press on it does. */
@Immutable
internal data class BubbleSelection(
    val isActive: Boolean = false,
    val isSelected: Boolean = false,
    val onToggle: () -> Unit = {},
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
    selection: BubbleSelection = BubbleSelection(),
) {
    val canReply = !message.isDeleted && message.status == MessageStatus.SENT && !selection.isActive
    ReplySwipe(enabled = canReply, onReply = { actions.onReply(message) }) {
        BubbleBody(message, showSenderName, downloads, actions, modifier, startsGroup, endsGroup, selection)
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
    selection: BubbleSelection,
) {
    val colors = bubbleColors(message)
    val showsAvatar = showSenderName && !message.isOwn
    val selectLabel = stringResource(R.string.message_select)
    val haptics = LocalHapticFeedback.current
    val highlight = if (selection.isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = SELECTED_ROW_ALPHA)
    } else {
        Color.Transparent
    }
    Column(
        modifier = modifier.fillMaxWidth()
            .background(highlight)
            .clickable(enabled = selection.isActive, onClickLabel = selectLabel, onClick = selection.onToggle)
            .padding(
                start = RowPadding,
                end = RowPadding,
                top = if (startsGroup) GroupSpacing else RowSpacing,
                bottom = RowSpacing,
            ),
        horizontalAlignment = if (message.isOwn) Alignment.End else Alignment.Start,
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            if (showsAvatar) SenderAvatarSlot(message.senderName, show = endsGroup)
            Box {
                Surface(
                    modifier = Modifier
                        .onLongPress(!message.isDeleted && !selection.isActive, selectLabel) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            selection.onToggle()
                        }
                        .semantics { selected = selection.isSelected },
                    shape = bubbleShape(message.isOwn, startsGroup),
                    color = colors.container,
                    contentColor = colors.content,
                ) {
                    BubbleContent(message, showSenderName && startsGroup, showsAvatar, downloads, actions)
                }
                // While messages are selected a tap chooses the message, also on a photo or a link inside it.
                if (selection.isActive) {
                    Box(
                        modifier = Modifier.matchParentSize()
                            .clickable(onClickLabel = selectLabel, onClick = selection.onToggle),
                    )
                }
            }
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
    hasAvatar: Boolean,
    downloads: AttachmentDownloadsUiState,
    actions: MessageActions,
) {
    val hasAttachments = !message.isDeleted && message.attachments.isNotEmpty()
    val maxWidth = maxBubbleWidth() - if (hasAvatar) AvatarSlotWidth else 0.dp
    if (message.isPhotoMessage()) {
        PhotoBubbleContent(message, showSenderName, maxWidth, downloads, actions)
        return
    }
    if (message.isEmojiOnlyMessage()) {
        Column(modifier = Modifier.padding(BubblePadding), horizontalAlignment = Alignment.End) {
            Text(text = message.body, style = LargeEmojiStyle)
            MessageMetadataLabel(message)
        }
        return
    }
    Column(modifier = Modifier.widthIn(max = maxWidth).padding(BubblePadding)) {
        message.replyTo?.let { QuotedReply(it, onClick = { id -> actions.onReplyClick(id) }) }
        if (hasAttachments) {
            MessageText(message, showSenderName)
            AttachmentList(
                message.attachments,
                downloads,
                actions.onAttachmentClick,
                actions.onImageClick,
                actions.onImageShown,
                actions.onRetryDownload,
            )
            MessageMetadataLabel(message, Modifier.align(Alignment.End))
        } else {
            Box {
                MessageText(message, showSenderName, endRoom = rememberMetadataWidth(message))
                MessageMetadataLabel(message, Modifier.align(Alignment.BottomEnd))
            }
        }
    }
}

/** Beside the last bubble of a person's run the avatar shows, and the other bubbles keep the same room empty. */
@Composable
private fun SenderAvatarSlot(name: String?, show: Boolean) {
    Box(modifier = Modifier.padding(end = AvatarGap).size(AvatarSize)) {
        if (show) InitialAvatar(name = name.orEmpty().ifBlank { "?" }, size = AvatarSize)
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
        message.isEmojiOnlyMessage() -> BubbleColors(Color.Transparent, scheme.onSurface)
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
