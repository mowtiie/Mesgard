package com.mowtiie.mesgard.ui.chats

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mowtiie.mesgard.R
import com.mowtiie.mesgard.domain.model.ConversationSummary
import com.mowtiie.mesgard.domain.model.ConversationType
import com.mowtiie.mesgard.ui.common.InitialAvatar
import com.mowtiie.mesgard.ui.common.SelectedAvatar
import com.mowtiie.mesgard.ui.common.fallbackTitleResource
import com.mowtiie.mesgard.ui.theme.MesgardTheme

private val MutedIconSize = 16.dp
private val StatusIconSpacing = 4.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ConversationRow(
    conversation: ConversationSummary,
    nowEpochMillis: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onLongClick: () -> Unit = {},
    typingNames: List<String> = emptyList(),
) {
    val title = conversation.title ?: stringResource(conversation.type.fallbackTitleResource())
    val hasUnread = conversation.hasUnread
    ListItem(
        modifier = modifier
            .combinedClickable(
                onClickLabel = stringResource(R.string.conversation_open),
                onLongClickLabel = stringResource(R.string.selection_select),
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .semantics { selected = isSelected },
        colors = if (isSelected) {
            ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            ListItemDefaults.colors()
        },
        leadingContent = { if (isSelected) SelectedAvatar() else InitialAvatar(title) },
        headlineContent = {
            Text(
                text = title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal,
            )
        },
        supportingContent = {
            Text(
                text = when {
                    typingNames.isNotEmpty() -> typingPreview(conversation.type, typingNames)
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
                if (conversation.isPinned || conversation.isMuted) StatusIcons(conversation)
            }
        },
    )
}

@Composable
private fun StatusIcons(conversation: ConversationSummary) {
    Row(horizontalArrangement = Arrangement.spacedBy(StatusIconSpacing)) {
        if (conversation.isPinned) PinnedIcon()
        if (conversation.isMuted) MutedIcon()
    }
}

@Composable
private fun PinnedIcon() {
    Icon(
        painter = painterResource(R.drawable.ic_push_pin),
        contentDescription = stringResource(R.string.conversation_pinned),
        modifier = Modifier.size(MutedIconSize),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun MutedIcon() {
    Icon(
        painter = painterResource(R.drawable.ic_notifications_off),
        contentDescription = stringResource(R.string.conversation_muted),
        modifier = Modifier.size(MutedIconSize),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** A conversation marked as unread has no count, so its badge is a plain dot. */
@Composable
private fun UnreadBadge(count: Int) {
    if (count == 0) {
        val description = stringResource(R.string.conversation_marked_unread)
        Badge(modifier = Modifier.semantics { contentDescription = description })
        return
    }
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
