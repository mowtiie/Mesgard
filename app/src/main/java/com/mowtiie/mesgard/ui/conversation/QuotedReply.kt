package com.mowtiie.mesgard.ui.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.ReplyPreview

private val QuotePadding = 8.dp
private val BarWidth = 3.dp
private const val QUOTE_MAX_LINES = 2

/** The message a reply answers, above its own text. Tapping it jumps to the original when that is on the device. */
@Composable
internal fun QuotedReply(reply: ReplyPreview, onClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .padding(bottom = QuotePadding)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = QUOTE_BACKGROUND_ALPHA))
            .clickable(enabled = reply.isLoaded, onClickLabel = stringResource(R.string.message_reply_show_original)) {
                onClick(reply.messageId)
            }
            .height(IntrinsicSize.Min),
    ) {
        Spacer(
            modifier = Modifier
                .width(BarWidth)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary),
        )
        Column(modifier = Modifier.padding(QuotePadding)) {
            reply.senderName?.let {
                Text(text = it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Text(
                text = quoteText(reply),
                style = MaterialTheme.typography.bodySmall,
                fontStyle = if (reply.body.isBlank() ||
                    reply.isDeleted ||
                    !reply.isLoaded
                ) {
                    FontStyle.Italic
                } else {
                    FontStyle.Normal
                },
                maxLines = QUOTE_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private const val QUOTE_BACKGROUND_ALPHA = 0.5f

@Composable
private fun quoteText(reply: ReplyPreview): String = when {
    !reply.isLoaded -> stringResource(R.string.message_reply_original)
    reply.isDeleted -> stringResource(R.string.message_deleted)
    reply.body.isBlank() -> stringResource(R.string.conversation_attachment_preview)
    else -> reply.body
}
