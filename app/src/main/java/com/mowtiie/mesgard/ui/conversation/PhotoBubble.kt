package com.mowtiie.mesgard.ui.conversation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.domain.model.Message

private val PhotoBubbleWidth = 260.dp
private val PhotoBubblePadding = 12.dp
private val OverlayInset = 6.dp

/** A message whose attachments are all photos: the photos fill the bubble, and the time sits over the last one. */
internal fun Message.isPhotoMessage(): Boolean =
    !isDeleted && attachments.isNotEmpty() && attachments.all { it.isImage }

/**
 * Photos go edge to edge, so the bubble has no padding around them. A reply, the sender and a caption keep the
 * padding of an ordinary bubble. Without a caption the time is drawn over the corner of the last photo.
 */
@Composable
internal fun PhotoBubbleContent(
    message: Message,
    showSenderName: Boolean,
    maxWidth: Dp,
    downloads: AttachmentDownloadsUiState,
    actions: MessageActions,
) {
    val name = message.senderName?.takeIf { showSenderName && !message.isOwn }
    val hasCaption = message.body.isNotEmpty()
    Column(modifier = Modifier.width(minOf(PhotoBubbleWidth, maxWidth))) {
        if (name != null || message.replyTo != null) {
            Column(
                modifier = Modifier.padding(
                    start = PhotoBubblePadding,
                    end = PhotoBubblePadding,
                    top = PhotoBubblePadding,
                ),
            ) {
                message.replyTo?.let { QuotedReply(it, onClick = { id -> actions.onReplyClick(id) }) }
                name?.let { SenderName(it) }
            }
        }
        Box {
            ImageGrid(
                attachments = message.attachments,
                downloads = downloads,
                onImageClick = actions.onImageClick,
                onImageShown = actions.onImageShown,
                onRetryDownload = actions.onRetryDownload,
            )
            if (!hasCaption) {
                MessageMetadataOverlay(
                    message,
                    Modifier.align(Alignment.BottomEnd).padding(OverlayInset),
                )
            }
        }
        if (hasCaption) {
            Box(modifier = Modifier.padding(PhotoBubblePadding)) {
                MessageText(message, showSenderName = false, endRoom = rememberMetadataWidth(message))
                MessageMetadataLabel(message, Modifier.align(Alignment.BottomEnd))
            }
        }
    }
}
