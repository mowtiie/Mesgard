package com.mowtiie.mesgard.ui.chats

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.ConversationSummary
import com.mowtiie.mesgard.domain.model.ConversationType
import com.mowtiie.mesgard.ui.theme.MesgardTheme

private val AvatarSize = 48.dp

@Composable
internal fun ConversationRow(conversation: ConversationSummary, nowEpochMillis: Long, modifier: Modifier = Modifier) {
    val title = conversation.title ?: stringResource(conversation.type.fallbackTitleResource())
    val hasUnread = conversation.unreadCount > 0
    ListItem(
        modifier = modifier,
        leadingContent = { Avatar(title) },
        headlineContent = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal,
            )
        },
        supportingContent = {
            Text(
                text = conversation.lastMessagePreview ?: stringResource(R.string.conversation_no_messages),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatActivityTime(conversation.lastActivityAtEpochMillis, nowEpochMillis),
                    style = MaterialTheme.typography.labelSmall,
                )
                if (hasUnread) UnreadBadge(conversation.unreadCount)
            }
        },
    )
}

@Composable
private fun UnreadBadge(count: Int) {
    val description = pluralStringResource(R.plurals.conversation_unread_messages, count, count)
    Badge(modifier = Modifier.semantics { contentDescription = description }) {
        Text(formatUnreadCount(count))
    }
}

@Composable
private fun Avatar(title: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(AvatarSize),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = title.firstOrNull()?.uppercase().orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@StringRes
private fun ConversationType.fallbackTitleResource(): Int = when (this) {
    ConversationType.DIRECT -> R.string.conversation_title_direct
    ConversationType.GROUP -> R.string.conversation_title_group
    ConversationType.ANNOUNCEMENT -> R.string.conversation_title_announcement
}

@Preview(showBackground = true)
@Composable
private fun ConversationRowPreview() {
    MesgardTheme {
        ConversationRow(
            conversation = ConversationSummary(
                id = "1",
                type = ConversationType.GROUP,
                title = "Grade 10 Section A",
                lastMessagePreview = "Reminder: bring your project proposals tomorrow.",
                lastActivityAtEpochMillis = System.currentTimeMillis(),
                unreadCount = 3,
                canPost = true,
            ),
            nowEpochMillis = System.currentTimeMillis(),
        )
    }
}
