package com.mowtiie.mesgard.ui.conversation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.Message
import com.mowtiie.mesgard.domain.model.MessageStatus

private val StatusIconSize = 14.dp
private val StatusIconGap = 4.dp
private val GapBeforeMetadata = 8.dp
private const val METADATA_ALPHA = 0.7f

/** The time of a message with its state: a clock while it waits, a check once the server has it. */
@Composable
internal fun MessageMetadataLabel(message: Message, modifier: Modifier = Modifier) {
    val text = metadata(message)
    val color = LocalContentColor.current.copy(alpha = METADATA_ALPHA)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(StatusIconGap),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = color)
        when {
            !message.isOwn -> Unit
            message.status == MessageStatus.SENT -> Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = stringResource(R.string.message_status_sent),
                modifier = Modifier.size(StatusIconSize),
                tint = color,
            )
            message.status == MessageStatus.PENDING -> Image(
                painter = painterResource(R.drawable.ic_schedule),
                contentDescription = null,
                modifier = Modifier.size(StatusIconSize),
                colorFilter = ColorFilter.tint(color),
            )
        }
    }
}

/**
 * The room the label takes at the end of the last line of the text, so the text wraps around it. The label is
 * drawn over this room, which keeps a short message and its time on one line.
 */
@Composable
internal fun rememberMetadataWidth(message: Message): Dp {
    val text = metadata(message)
    val style = MaterialTheme.typography.labelSmall
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val hasStatusIcon = message.isOwn && message.status != MessageStatus.FAILED
    return remember(text, style, hasStatusIcon, density) {
        val textWidth = with(density) { measurer.measure(text, style).size.width.toDp() }
        val iconWidth = if (hasStatusIcon) StatusIconGap + StatusIconSize else 0.dp
        textWidth + iconWidth + GapBeforeMetadata
    }
}
