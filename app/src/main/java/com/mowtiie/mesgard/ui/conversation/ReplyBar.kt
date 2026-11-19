package com.mowtiie.mesgard.ui.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R

private val BarPadding = 12.dp
private const val BAR_MAX_LINES = 1

/** Above the composer while a reply is being written, with the way to drop it. */
@Composable
internal fun ReplyBar(target: ReplyTarget, onDrop: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            modifier = Modifier.padding(start = BarPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BarPadding),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.message_replying_to, target.senderName.orEmpty()),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = target.text.ifBlank { stringResource(R.string.conversation_attachment_preview) },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = BAR_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onDrop) {
                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.message_reply_cancel))
            }
        }
    }
}
