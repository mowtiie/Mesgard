package com.mowtiie.mesgard.ui.conversation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.PinnedMessage

private val BarPadding = 12.dp
private val PinIconSize = 18.dp

/**
 * Under the title while something is pinned: the latest pinned message, which jumps to it. With several, each tap goes
 * on to the next one, as the count says.
 */
@Composable
internal fun PinnedMessageBar(pins: List<PinnedMessage>, onJump: (String) -> Unit, modifier: Modifier = Modifier) {
    if (pins.isEmpty()) return
    var shown by remember(pins.size) { mutableIntStateOf(0) }
    val pin = pins[shown.coerceIn(pins.indices)]
    val title = if (pins.size > 1) {
        stringResource(R.string.pinned_message_count, shown.coerceIn(pins.indices) + 1, pins.size)
    } else {
        stringResource(R.string.pinned_message_title)
    }
    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClickLabel = stringResource(R.string.pinned_message_show)) {
            onJump(pin.messageId)
            shown = (shown + 1) % pins.size
        },
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(modifier = Modifier.padding(BarPadding), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.PushPin,
                contentDescription = null,
                modifier = Modifier.size(PinIconSize),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.padding(start = BarPadding)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "${pin.senderName}: ${pin.body.ifBlank {
                        stringResource(R.string.conversation_attachment_preview)
                    }}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
