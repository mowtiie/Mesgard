package com.mowtiie.mesgard.ui.announcements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.Message
import com.mowtiie.mesgard.ui.chats.formatActivityTime
import com.mowtiie.mesgard.ui.common.InitialAvatar

private const val MAX_BODY_LINES = 8
private val CardPadding = 16.dp
private val ItemSpacing = 12.dp
private val AvatarSize = 40.dp

@Composable
internal fun AnnouncementCard(
    announcement: Message,
    nowEpochMillis: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sender = announcement.senderName ?: stringResource(R.string.conversation_title_former_member)
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(CardPadding), verticalArrangement = Arrangement.spacedBy(ItemSpacing)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ItemSpacing),
            ) {
                InitialAvatar(sender, size = AvatarSize)
                Column {
                    Text(text = sender, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = formatActivityTime(announcement.createdAtEpochMillis, nowEpochMillis),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (announcement.body.isNotBlank()) {
                Text(
                    text = announcement.body,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = MAX_BODY_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (announcement.attachments.isNotEmpty()) {
                val count = announcement.attachments.size
                Text(
                    text = pluralStringResource(R.plurals.announcement_attachments, count, count),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
