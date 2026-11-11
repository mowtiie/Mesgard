package com.mowtiie.mesgard.ui.chats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Badge
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.ConversationSummary
import com.mowtiie.mesgard.domain.model.ConversationType
import com.mowtiie.mesgard.ui.common.InitialAvatar
import com.mowtiie.mesgard.ui.common.fallbackTitleResource
import com.mowtiie.mesgard.ui.theme.MesgardTheme

@Composable
internal fun ConversationRow(
    conversation: ConversationSummary,
    nowEpochMillis: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = conversation.title ?: stringResource(conversation.type.fallbackTitleResource())
    val hasUnread = conversation.unreadCount > 0
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        leadingContent = { InitialAvatar(title) },
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
                text = when {
                    conversation.lastMessagePreview == null -> stringResource(R.string.conversation_no_messages)
                    conversation.lastMessagePreview.isBlank() -> stringResource(
                        R.string.conversation_attachment_preview,
                    )
                    else -> conversation.lastMessagePreview
                },
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
            onClick = {},
        )
    }
}
